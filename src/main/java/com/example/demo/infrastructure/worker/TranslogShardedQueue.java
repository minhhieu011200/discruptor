package com.example.demo.infrastructure.worker;

import com.example.demo.domain.entity.TranslogEntity;
import com.example.demo.domain.repository.TranslogShardedQueueRepository;

import io.netty.util.internal.shaded.org.jctools.queues.MpmcArrayQueue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TranslogShardedQueue implements TranslogShardedQueueRepository {

    private final int queueCapacity;
    private final Map<String, MpmcArrayQueue<TranslogEntity>> shardedQueues = new ConcurrentHashMap<>();

    public TranslogShardedQueue(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public MpmcArrayQueue<TranslogEntity> getQueue(String symbol) {
        return shardedQueues.computeIfAbsent(symbol, s -> new MpmcArrayQueue<>(queueCapacity));
    }

    public void offer(TranslogEntity entity) {
        MpmcArrayQueue<TranslogEntity> queue = getQueue(entity.getImtcode());
        while (!queue.offer(entity)) {
            Thread.yield(); // spin-wait nếu queue đầy
        }
    }

    public Map<String, MpmcArrayQueue<TranslogEntity>> getShardedQueues() {
        return shardedQueues;
    }
}