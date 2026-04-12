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
import org.springframework.beans.factory.annotation.Value;

@Repository
public class TranslogQueue implements TranslogShardedQueueRepository {
    private static final int BATCH_SIZE = 5000;
    private static final int QUEUE_CAPACITY = 50000;

    private final long flushMillis; // flush nếu item tồn > threshold

    private final MpmcArrayQueue<TranslogEntity> sharedQueue = new MpmcArrayQueue<>(QUEUE_CAPACITY);
    private final int workerCount;
    private final ExecutorService executor;
    private final TranslogMapper mapper;
    private final TransLogWorker[] workers;

    public TranslogQueue(TranslogMapper mapper,
            @Value("${translog.worker.count:4}") int workerCount,
            @Value("${translog.flush.millis:1000}") long flushMillis) {
        this.mapper = mapper;
        this.workerCount = workerCount;
        this.flushMillis = flushMillis;
        this.executor = Executors.newFixedThreadPool(workerCount);
        this.workers = new TransLogWorker[workerCount];

        // khởi cố định pool worker xử lý batch từ queue chung
        for (int i = 0; i < workerCount; i++) {
            TransLogWorker worker = new TransLogWorker(sharedQueue, mapper, BATCH_SIZE, this.flushMillis);
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