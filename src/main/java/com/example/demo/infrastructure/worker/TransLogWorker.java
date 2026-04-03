package com.example.demo.infrastructure.worker;

import com.example.demo.domain.entity.TranslogEntity;
import com.example.demo.infrastructure.mybatis.TranslogMapper;

import io.netty.util.internal.shaded.org.jctools.queues.MpmcArrayQueue;

import java.util.ArrayList;
import java.util.List;

public class TransLogWorker implements Runnable {

    private final MpmcArrayQueue<TranslogEntity> queue;
    private final TranslogMapper mapper;
    private final int batchSize;
    private final long maxWaitMillis;

    private volatile boolean running = true;

    public TransLogWorker(MpmcArrayQueue<TranslogEntity> queue,
            TranslogMapper mapper,
            int batchSize,
            long maxWaitMillis) {
        this.queue = queue;
        this.mapper = mapper;
        this.batchSize = batchSize;
        this.maxWaitMillis = maxWaitMillis;
    }

    public void shutdown() {
        running = false;
    }

    @Override
    public void run() {

        List<TranslogEntity> batch = new ArrayList<>(batchSize);
        long firstAddTime = 0;

        int emptyPollCount = 0;

        while (running || !queue.isEmpty() || !batch.isEmpty()) {

            TranslogEntity item = queue.poll();
            long now = System.currentTimeMillis();

            if (item != null) {
                emptyPollCount = 0;

                if (batch.isEmpty()) {
                    firstAddTime = now;
                }

                batch.add(item);

                if (batch.size() >= batchSize) {
                    flushBatch(batch);
                    batch.clear();
                    firstAddTime = 0;
                }

                continue;
            }

            // Queue rỗng → kiểm tra timeout batch
            if (!batch.isEmpty() &&
                    maxWaitMillis > 0 &&
                    now - firstAddTime >= maxWaitMillis) {

                flushBatch(batch);
                batch.clear();
                firstAddTime = 0;
                continue;
            }

            // Avoid busy spin
            emptyPollCount++;
            if (emptyPollCount > 50) {
                emptyPollCount = 0;
                Thread.yield();
            }
        }

        // Before exit → flush hết
        if (!batch.isEmpty()) {
            flushBatch(batch);
        }
    }

    private void flushBatch(List<TranslogEntity> batch) {
        if (!batch.isEmpty()) {
            try {
                mapper.insertBatch(batch);
            } catch (Exception e) {
                // TODO: retry hoặc đưa vào DLQ
                e.printStackTrace();
            }
        }
    }

}