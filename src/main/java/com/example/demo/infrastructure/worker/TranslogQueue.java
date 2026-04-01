package com.example.demo.infrastructure.worker;

import com.example.demo.domain.entity.TranslogEntity;
import com.example.demo.domain.repository.TranslogShardedQueueRepository;
import com.example.demo.infrastructure.mybatis.TranslogMapper;

import io.netty.util.internal.shaded.org.jctools.queues.MpmcArrayQueue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Repository;

@Repository
public class TranslogQueue implements TranslogShardedQueueRepository {
    private static final int BATCH_SIZE = 5000;
    private static final int WORKER_COUNT = 16; // số thread tiêu thụ chung
    private static final int QUEUE_CAPACITY = 10_000;
    private static final long FLUSH_MILLIS = 500; // flush nếu item tồn > 500

    private final MpmcArrayQueue<TranslogEntity> sharedQueue = new MpmcArrayQueue<>(QUEUE_CAPACITY);
    private final ExecutorService executor = Executors.newFixedThreadPool(WORKER_COUNT);
    private final TranslogMapper mapper;

    public TranslogQueue(TranslogMapper mapper) {
        this.mapper = mapper;
        // khởi cố định pool worker xử lý batch từ queue chung
        for (int i = 0; i < WORKER_COUNT; i++) {
            executor.submit(new TransLogWorker(sharedQueue, mapper, BATCH_SIZE, FLUSH_MILLIS));
        }
    }

    public void offer(TranslogEntity entity) {
        // đưa vào queue chung
        while (!sharedQueue.offer(entity)) {
            Thread.yield();
        }
    }

    public MpmcArrayQueue<TranslogEntity> getSharedQueue() {
        return sharedQueue;
    }
}