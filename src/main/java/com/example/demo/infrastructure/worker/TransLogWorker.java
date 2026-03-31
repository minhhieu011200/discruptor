package com.example.demo.infrastructure.worker;

import com.example.demo.domain.entity.TranslogEntity;
import com.example.demo.infrastructure.mybatis.TranslogMapper;

import io.netty.util.internal.shaded.org.jctools.queues.MpmcArrayQueue;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

public class TransLogWorker implements Runnable {
    private final MpmcArrayQueue<TranslogEntity> queue;
    private final TranslogMapper mapper;
    private final int batchSize;

    public TransLogWorker(MpmcArrayQueue<TranslogEntity> queue, TranslogMapper mapper, int batchSize) {
        this.queue = queue;
        this.mapper = mapper;
        this.batchSize = batchSize;
    }

    @Override
    public void run() {
        List<TranslogEntity> batch = new ArrayList<>(batchSize);

        while (true) {
            TranslogEntity entity = queue.poll();

            if (entity != null) {
                batch.add(entity);

                if (batch.size() >= batchSize) {
                    insertBatch(batch);
                    batch.clear();
                }
            } else {
                if (!batch.isEmpty()) {

                    insertBatch(batch);
                    batch.clear();
                }
                Thread.yield(); // không có dữ liệu → yield CPU
            }
        }
    }

    // @Transactional
    public void insertBatch(List<TranslogEntity> batch) {
        if (!batch.isEmpty()) {
            mapper.insertBatch(batch);
        }
    }
}