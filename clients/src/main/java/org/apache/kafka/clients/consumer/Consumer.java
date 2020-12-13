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
package org.apache.kafka.clients.consumer;

import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;

import java.io.Closeable;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @see KafkaConsumer
 * @see MockConsumer
 */
public interface Consumer<K, V> extends Closeable {

    /**
     * @see KafkaConsumer#assignment()
     * 获取该消费者的队列分配列表。
     */
    Set<TopicPartition> assignment();

    /**
     * @see KafkaConsumer#subscription()
     * 获取该消费者的订阅信息。
     */
    Set<String> subscription();

    /**
     * @see KafkaConsumer#subscribe(Collection)
     * 订阅指定topic，并为消费者自动分配分区
     */
    void subscribe(Collection<String> topics);

    /**
     * @see KafkaConsumer#subscribe(Collection, ConsumerRebalanceListener)
     * 订阅主题，并指定队列重平衡的监听器。
     */
    void subscribe(Collection<String> topics, ConsumerRebalanceListener callback);

    /**
     * @see KafkaConsumer#assign(Collection)
     * 取代 subscription，手动指定消费哪些队列。
     */
    void assign(Collection<TopicPartition> partitions);

    /**
    * @see KafkaConsumer#subscribe(Pattern, ConsumerRebalanceListener)
    */
    void subscribe(Pattern pattern, ConsumerRebalanceListener callback);

    /**
    * @see KafkaConsumer#subscribe(Pattern)
    */
    void subscribe(Pattern pattern);

    /**
     * @see KafkaConsumer#unsubscribe()
     * 取消订阅关系。
     */
    void unsubscribe();

    /**
     * @see KafkaConsumer#poll(long)
     */
    @Deprecated
    ConsumerRecords<K, V> poll(long timeout);

    /**
     * @see KafkaConsumer#poll(Duration)
     * 拉取消息，是 KafkaConsumer 的核心方法
     */
    ConsumerRecords<K, V> poll(Duration timeout);

    /**
     * @see KafkaConsumer#commitSync()
     * 同步提交消费进度，为本批次的消费提交，
     */
    void commitSync();

    /**
     * @see KafkaConsumer#commitSync(Duration)
     * 同步提交消费进度，可设置超时时间。
     */
    void commitSync(Duration timeout);

    /**
     * @see KafkaConsumer#commitSync(Map)
     * 显示同步提交消费进度， offsets 指明需要提交消费进度的信息
     */
    void commitSync(Map<TopicPartition, OffsetAndMetadata> offsets);

    /**
     * @see KafkaConsumer#commitSync(Map, Duration)
     * 显示同步提交消费进度，带超时间。
     */
    void commitSync(final Map<TopicPartition, OffsetAndMetadata> offsets, final Duration timeout);
    /**
     * @see KafkaConsumer#commitAsync()
     */
    void commitAsync();

    /**
     * @see KafkaConsumer#commitAsync(OffsetCommitCallback)
     */
    void commitAsync(OffsetCommitCallback callback);

    /**
     * @see KafkaConsumer#commitAsync(Map, OffsetCommitCallback)
     */
    void commitAsync(Map<TopicPartition, OffsetAndMetadata> offsets, OffsetCommitCallback callback);

    /**
     * @see KafkaConsumer#seek(TopicPartition, long)
     * 重置 consumer#poll 方法下一次拉消息的偏移量。
     */
    void seek(TopicPartition partition, long offset);

    /**
     * @see KafkaConsumer#seek(TopicPartition, OffsetAndMetadata)
     */
    void seek(TopicPartition partition, OffsetAndMetadata offsetAndMetadata);

    /**
     * @see KafkaConsumer#seekToBeginning(Collection)
     * 将 poll 方法下一次的拉取偏移量设置为队列的初始偏移量。
     */
    void seekToBeginning(Collection<TopicPartition> partitions);

    /**
     * @see KafkaConsumer#seekToEnd(Collection)
     * 将 poll 方法下一次的拉取偏移量设置为队列的最大偏移量
     */
    void seekToEnd(Collection<TopicPartition> partitions);

    /**
     * @see KafkaConsumer#position(TopicPartition)
     * 获取将被拉取的偏移量。
     */
    long position(TopicPartition partition);
    
    /**
     * @see KafkaConsumer#position(TopicPartition, Duration)
     */
    long position(TopicPartition partition, final Duration timeout);

    /**
     * @see KafkaConsumer#committed(TopicPartition)
     */
    @Deprecated
    OffsetAndMetadata committed(TopicPartition partition);

    /**
     * @see KafkaConsumer#committed(TopicPartition, Duration)
     */
    @Deprecated
    OffsetAndMetadata committed(TopicPartition partition, final Duration timeout);

    /**
     * @see KafkaConsumer#committed(Set)
     * 获取指定分区已提交的偏移量。
     */
    Map<TopicPartition, OffsetAndMetadata> committed(Set<TopicPartition> partitions);

    /**
     * @see KafkaConsumer#committed(Set, Duration)
     */
    Map<TopicPartition, OffsetAndMetadata> committed(Set<TopicPartition> partitions, final Duration timeout);

    /**
     * @see KafkaConsumer#metrics()
     */
    Map<MetricName, ? extends Metric> metrics();

    /**
     * @see KafkaConsumer#partitionsFor(String)
     * 获取主题的路由信息。
     */
    List<PartitionInfo> partitionsFor(String topic);

    /**
     * @see KafkaConsumer#partitionsFor(String, Duration)
     */
    List<PartitionInfo> partitionsFor(String topic, Duration timeout);

    /**
     * @see KafkaConsumer#listTopics()
     * 获取所有 topic 的路由信息。
     */
    Map<String, List<PartitionInfo>> listTopics();

    /**
     * @see KafkaConsumer#listTopics(Duration)
     */
    Map<String, List<PartitionInfo>> listTopics(Duration timeout);

    /**
     * @see KafkaConsumer#paused()
     * 获取已挂起的分区信息。
     */
    Set<TopicPartition> paused();

    /**
     * @see KafkaConsumer#pause(Collection)
     * 挂起分区，下一次 poll 方法将不会返回这些分区的消息。
     */
    void pause(Collection<TopicPartition> partitions);

    /**
     * @see KafkaConsumer#resume(Collection)
     * 恢复挂起的分区。
     */
    void resume(Collection<TopicPartition> partitions);

    /**
     * @see KafkaConsumer#offsetsForTimes(Map)
     * 根据时间戳查找最近的一条消息的偏移量。
     */
    Map<TopicPartition, OffsetAndTimestamp> offsetsForTimes(Map<TopicPartition, Long> timestampsToSearch);

    /**
     * @see KafkaConsumer#offsetsForTimes(Map, Duration)
     */
    Map<TopicPartition, OffsetAndTimestamp> offsetsForTimes(Map<TopicPartition, Long> timestampsToSearch, Duration timeout);

    /**
     * @see KafkaConsumer#beginningOffsets(Collection)
     * 查询指定分区当前最小的偏移量。
     */
    Map<TopicPartition, Long> beginningOffsets(Collection<TopicPartition> partitions);

    /**
     * @see KafkaConsumer#beginningOffsets(Collection, Duration)
     */
    Map<TopicPartition, Long> beginningOffsets(Collection<TopicPartition> partitions, Duration timeout);

    /**
     * @see KafkaConsumer#endOffsets(Collection)
     * 查询指定分区当前最大的偏移量。
     */
    Map<TopicPartition, Long> endOffsets(Collection<TopicPartition> partitions);

    /**
     * @see KafkaConsumer#endOffsets(Collection, Duration)
     */
    Map<TopicPartition, Long> endOffsets(Collection<TopicPartition> partitions, Duration timeout);

    /**
     * @see KafkaConsumer#groupMetadata()
     */
    ConsumerGroupMetadata groupMetadata();

    /**
     * @see KafkaConsumer#enforceRebalance()
     */
    void enforceRebalance();

    /**
     * @see KafkaConsumer#close()
     * 关闭消费者。
     */
    void close();

    /**
     * @see KafkaConsumer#close(long, TimeUnit)
     */
    @Deprecated
    void close(long timeout, TimeUnit unit);

    /**
     * @see KafkaConsumer#close(Duration)
     */
    void close(Duration timeout);

    /**
     * @see KafkaConsumer#wakeup()
     * 唤醒消费者。
     */
    void wakeup();

}
