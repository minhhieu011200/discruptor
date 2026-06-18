package com.example.demo.infrastructure.worker;

import com.example.demo.domain.entity.TranslogEntity;
import com.example.demo.domain.repository.TranslogShardedQueueRepository;
import com.example.demo.infrastructure.mybatis.TranslogMapper;

import io.netty.util.internal.shaded.org.jctools.queues.MpmcArrayQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.LockSupport;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransLogWorker implements Runnable {

    private final TranslogShardedQueueRepository queue;
    private final TranslogMapper mapper;
    private final int batchSize;
    private final long maxWaitMillis;
    private final ExecutorService flushExecutor;
    private volatile boolean running = true;

    public TransLogWorker(TranslogShardedQueueRepository queue,
            TranslogMapper mapper,
            int batchSize,
            long maxWaitMillis,
            ExecutorService flushExecutor) {

        this.queue = queue;
        this.mapper = mapper;
        this.batchSize = batchSize;
        this.maxWaitMillis = maxWaitMillis;
        this.flushExecutor = flushExecutor;
    }

    public void shutdown() {
        running = false;
    }

    @Override
    public void run() {

        List<TranslogEntity> batch = new ArrayList<>(batchSize);
        long firstAddTime = 0;
        long timeoutNanos = maxWaitMillis * 1_000_000L;

        while (running || !queue.isEmpty() || !batch.isEmpty()) {

            TranslogEntity item = queue.poll();
            long now = System.nanoTime();

            if (item != null) {

                if (batch.isEmpty()) {
                    firstAddTime = now;
                }
                batch.add(item);

                if (batch.size() >= batchSize) {
                    flush(batch);
                    // PERF: tạo list mới thay vì clear – flush() đã giữ reference cũ
                    batch = new ArrayList<>(batchSize);
                }
                continue;
            }

            // flush theo timeout
            if (!batch.isEmpty() && timeoutNanos > 0 &&
                    now - firstAddTime >= timeoutNanos) {

                flush(batch);
                batch = new ArrayList<>(batchSize);
                continue;
            }

            // PERF: parkNanos(100) thay cho Thread.yield()
            // yield() vẫn consume CPU trên nhiều OS; parkNanos giải phóng core hơn
            LockSupport.parkNanos(100L);
        }

        // flush cuối
        if (!batch.isEmpty()) {
            flush(batch);
        }
    }

    private void flush(List<TranslogEntity> batch) {
        // PERF: giữ nguyên reference cũ – caller tạo list mới sau khi gọi flush()
        // Nên không cần copy để tránh batch bị mutate
        final List<TranslogEntity> toFlush = batch;
        try {
            flushExecutor.submit(() -> {
                try {
                    mapper.insertBatch(toFlush);
                } catch (Exception e) {
                    log.error("[flush error] {}", e.getMessage());
                    // TODO: retry hoặc đẩy DLQ
                }
            });
        } catch (RejectedExecutionException ex) {
            // queue quá tải → nên chuyển sang DLQ
            log.error("[flush rejected] executor overload, dropping {} records", toFlush.size());
        }
    }
}