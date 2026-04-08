package com.example.demo.infrastructure.worker;

import com.example.demo.domain.entity.TranslogEntity;
import com.example.demo.domain.repository.TranslogShardedQueueRepository;
import com.example.demo.infrastructure.mybatis.TranslogMapper;

import io.netty.util.internal.shaded.org.jctools.queues.MpmcArrayQueue;
import jakarta.annotation.PreDestroy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Repository;

@Repository
public class TranslogQueue implements TranslogShardedQueueRepository {
    private static final int BATCH_SIZE = 10000;
    private static final int WORKER_COUNT = 4; // số thread tiêu thụ chung
    private static final int QUEUE_CAPACITY = 1000000;
    private static final long FLUSH_MILLIS = 1000; // flush nếu item tồn > 500

    private final MpmcArrayQueue<TranslogEntity> sharedQueue = new MpmcArrayQueue<>(QUEUE_CAPACITY);
    private final ExecutorService executor = Executors.newFixedThreadPool(WORKER_COUNT);
    private final TranslogMapper mapper;
    private final TransLogWorker[] workers = new TransLogWorker[WORKER_COUNT];

    public TranslogQueue(TranslogMapper mapper) {
        this.mapper = mapper;
        // khởi cố định pool worker xử lý batch từ queue chung
        for (int i = 0; i < WORKER_COUNT; i++) {
            TransLogWorker worker = new TransLogWorker(sharedQueue, mapper, BATCH_SIZE, FLUSH_MILLIS);
            workers[i] = worker;
            executor.submit(worker);
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

    @PreDestroy
    public void shutdown() {
        System.out.println("[TranslogQueue] Shutdown requested...");

        // 1) báo worker dừng nhận mới
        for (TransLogWorker w : workers) {
            w.shutdown();
        }

        // 2) dừng executor (không nhận job mới)
        executor.shutdown();

        try {
            // 3) chờ worker xử lý hết queue + flush batch còn lại
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                System.out.println("[TranslogQueue] Force shutting down workers...");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("[TranslogQueue] Stopped cleanly.");
    }
}