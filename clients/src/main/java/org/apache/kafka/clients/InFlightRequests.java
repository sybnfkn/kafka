/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kafka.clients;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The set of requests which have been sent or are being sent but haven't yet received a response
 * ****缓存已经发出去但是没有收到响应的Clientrequest****，通过Map<String, Deque<NetworkClient.InFlightRequest>> requests
 * 通过配置可以限制InFlightRequest个数
 */
final class InFlightRequests {

    private final int maxInFlightRequestsPerConnection;
    // 每个node都会 维护一个 InFlightRequest Deque
    private final Map<String, Deque<NetworkClient.InFlightRequest>> requests = new HashMap<>();
    /** Thread safe total number of in flight requests. */
    private final AtomicInteger inFlightRequestCount = new AtomicInteger(0);

    public InFlightRequests(int maxInFlightRequestsPerConnection) {
        this.maxInFlightRequestsPerConnection = maxInFlightRequestsPerConnection;
    }

    /**
     * Add the given request to the queue for the connection it was directed to
     */
    public void add(NetworkClient.InFlightRequest request) {
        String destination = request.destination;
        Deque<NetworkClient.InFlightRequest> reqs = this.requests.get(destination);
        if (reqs == null) {
            reqs = new ArrayDeque<>();
            this.requests.put(destination, reqs);
        }
        // 加到头部节点
        reqs.addFirst(request);
        inFlightRequestCount.incrementAndGet();
    }

    /**
     * Get the request queue for the given node
     */
    private Deque<NetworkClient.InFlightRequest> requestQueue(String node) {
        Deque<NetworkClient.InFlightRequest> reqs = requests.get(node);
        if (reqs == null || reqs.isEmpty())
            throw new IllegalStateException("There are no in-flight requests for node " + node);
        return reqs;
    }

    /**
     * Get the oldest request (the one that will be completed next) for the given node
     */
    public NetworkClient.InFlightRequest completeNext(String node) {
        // 移除出去
        NetworkClient.InFlightRequest inFlightRequest = requestQueue(node).pollLast();
        inFlightRequestCount.decrementAndGet();
        return inFlightRequest;
    }

    /**
     * Get the last request we sent to the given node (but don't remove it from the queue)
     * @param node The node id
     */
    public NetworkClient.InFlightRequest lastSent(String node) {
        return requestQueue(node).peekFirst();
    }

    /**
     * Complete the last request that was sent to a particular node.
     * @param node The node the request was sent to
     * @return The request
     */
    public NetworkClient.InFlightRequest completeLastSent(String node) {
        // 将inFlightrequest中对应队列第一个请求删掉
        NetworkClient.InFlightRequest inFlightRequest = requestQueue(node).pollFirst();
        inFlightRequestCount.decrementAndGet();
        return inFlightRequest;
    }

    /**
     * Can we send more requests to this node?
     *
     * @param node Node in question
     * @return true iff we have no requests still being sent to the given node
     * queue.peekFirst().send.completed() = true 标示当前队头请求已经发送完成
     * 队头消息和对应KafkaChannel.send字段指向同一个消息，为了避免未发送的消息被覆盖，也不能让KafkaChannel.send 字段指向新请求
     * queue.size() < this.maxInFlightRequestsPerConnection 为了判断InFlightRequest队列中是否堆积过多请求。
     * 如果node对应队积累很多未响应请求，说明这个节点负载比较大，或者网络有问题，继续发送的话可能导致请求超时
     *
     * 1。node 的queue为空
     * 2。node的queue的头节点已经发送完毕 并且 queue.size < 阈值
     *
     * ********如果之前发送的请求并没有通过底层socket实际发送完成, 是不允许发送新的request的
     */
    public boolean canSendMore(String node) {
        Deque<NetworkClient.InFlightRequest> queue = requests.get(node);
        return queue == null || queue.isEmpty() ||
                // 头节点是否已经发送完毕
               (queue.peekFirst().send.completed() && queue.size() < this.maxInFlightRequestsPerConnection);
    }

    /**
     * Return the number of in-flight requests directed at the given node
     * @param node The node
     * @return The request count.
     */
    public int count(String node) {
        Deque<NetworkClient.InFlightRequest> queue = requests.get(node);
        return queue == null ? 0 : queue.size();
    }

    /**
     * Return true if there is no in-flight request directed at the given node and false otherwise
     */
    public boolean isEmpty(String node) {
        Deque<NetworkClient.InFlightRequest> queue = requests.get(node);
        return queue == null || queue.isEmpty();
    }

    /**
     * Count all in-flight requests for all nodes. This method is thread safe, but may lag the actual count.
     */
    public int count() {
        return inFlightRequestCount.get();
    }

    /**
     * Return true if there is no in-flight request and false otherwise
     */
    public boolean isEmpty() {
        for (Deque<NetworkClient.InFlightRequest> deque : this.requests.values()) {
            if (!deque.isEmpty())
                return false;
        }
        return true;
    }

    /**
     * Clear out all the in-flight requests for the given node and return them
     *
     * @param node The node
     * @return All the in-flight requests for that node that have been removed
     */
    public Iterable<NetworkClient.InFlightRequest> clearAll(String node) {
        Deque<NetworkClient.InFlightRequest> reqs = requests.get(node);
        if (reqs == null) {
            return Collections.emptyList();
        } else {
            final Deque<NetworkClient.InFlightRequest> clearedRequests = requests.remove(node);
            inFlightRequestCount.getAndAdd(-clearedRequests.size());
            return () -> clearedRequests.descendingIterator();
        }
    }

    private Boolean hasExpiredRequest(long now, Deque<NetworkClient.InFlightRequest> deque) {
        for (NetworkClient.InFlightRequest request : deque) {
            long timeSinceSend = Math.max(0, now - request.sendTimeMs);
            if (timeSinceSend > request.requestTimeoutMs)
                return true;
        }
        return false;
    }

    /**
     * Returns a list of nodes with pending in-flight request, that need to be timed out
     *
     * @param now current time in milliseconds
     * @return list of nodes
     */
    public List<String> nodesWithTimedOutRequests(long now) {
        List<String> nodeIds = new ArrayList<>();
        for (Map.Entry<String, Deque<NetworkClient.InFlightRequest>> requestEntry : requests.entrySet()) {
            String nodeId = requestEntry.getKey();
            Deque<NetworkClient.InFlightRequest> deque = requestEntry.getValue();
            // 已经超时了
            if (hasExpiredRequest(now, deque))
                nodeIds.add(nodeId);
        }
        return nodeIds;
    }

}
