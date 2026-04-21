package com.example.demo.infrastructure.worker;

import com.example.demo.domain.entity.TranslogEntity;
import com.example.demo.domain.repository.TranslogShardedQueueRepository;
import com.example.demo.infrastructure.mybatis.TranslogMapper;

import io.netty.util.internal.shaded.org.jctools.queues.MpmcArrayQueue;

import java.util.ArrayList;
import java.util.List;

public class TransLogWorker implements Runnable {

    private final TranslogShardedQueueRepository queue;
    private final int partition;
    private final TranslogMapper mapper;
    private final int batchSize;
    private final long maxWaitMillis;

    private volatile boolean running = true;

    public TransLogWorker(TranslogShardedQueueRepository queue,
            int partition,
            TranslogMapper mapper,
            int batchSize,
            long maxWaitMillis) {

        this.queue = queue;
        this.partition = partition;
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

        int idle = 0;

        while (running || !queue.isEmpty() || !batch.isEmpty()) {

            TranslogEntity item = queue.poll(partition);
            long now = System.currentTimeMillis();

            if (item != null) {
                idle = 0;

                if (batch.isEmpty()) {
                    firstAddTime = now;
                }

                batch.add(item);

                if (batch.size() >= batchSize) {
                    flush(batch);
                    batch.clear();
                }

                continue;
            }

            // flush theo timeout
            if (!batch.isEmpty() &&
                    maxWaitMillis > 0 &&
                    now - firstAddTime >= maxWaitMillis) {

                flush(batch);
                batch.clear();
                continue;
            }

            // idle strategy (hybrid)
            idle++;
            if (idle < 50) {
                Thread.onSpinWait();
            } else if (idle < 100) {
                Thread.yield();
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // flush cuối
        if (!batch.isEmpty()) {
            flush(batch);
        }
    }

    private void flush(List<TranslogEntity> batch) {
        try {
            mapper.insertBatch(batch);
        } catch (Exception e) {
            // TODO: retry / DLQ
            System.err.println("[TransLogWorker] flush error: " + e.getMessage());
        }
    }
}