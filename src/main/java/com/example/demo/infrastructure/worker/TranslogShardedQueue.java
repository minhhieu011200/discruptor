package com.example.demo.infrastructure.worker;

import com.example.demo.domain.entity.TranslogEntity;
import com.example.demo.domain.repository.TranslogShardedQueueRepository;
import com.example.demo.infrastructure.mybatis.TranslogMapper;

import io.netty.util.internal.shaded.org.jctools.queues.MpmcArrayQueue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Repository;

@Repository
public class TranslogShardedQueue implements TranslogShardedQueueRepository {
    private static final int BATCH_SIZE = 5000;
    private static final int WORKERS_PER_SHARD = 4;
    private final Map<String, Boolean> startedWorkers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private static final int QUEUE_CAPACITY = 10_000;
    private final Map<String, MpmcArrayQueue<TranslogEntity>> shardedQueues = new ConcurrentHashMap<>();
    private final TranslogMapper mapper;

    public TranslogShardedQueue(TranslogMapper mapper) {
        this.mapper = mapper;
    }

    public void offer(TranslogEntity entity) {
        String symbol = entity.getImtcode();
        // tạo shard queue nếu chưa có
        MpmcArrayQueue<TranslogEntity> queue = shardedQueues.computeIfAbsent(symbol,
                s -> new MpmcArrayQueue<>(QUEUE_CAPACITY));
        // push dữ liệu
        while (!queue.offer(entity)) {
            Thread.yield();
        }
        // start worker dynamic nếu chưa chạy
        startedWorkers.computeIfAbsent(symbol, s -> {
            for (int i = 0; i < WORKERS_PER_SHARD; i++) {
                executor.submit(new TransLogWorker(queue, mapper, BATCH_SIZE));
            }
            System.out.println("Started workers for symbol: " + symbol);
            return true;
        });
    }

    public Map<String, MpmcArrayQueue<TranslogEntity>> getShardedQueues() {
        return shardedQueues;
    }
}