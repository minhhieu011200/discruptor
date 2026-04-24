package com.example.demo.infrastructure.worker;

import com.example.demo.domain.entity.TranslogEntity;
import com.example.demo.domain.repository.TranslogShardedQueueRepository;
import com.example.demo.infrastructure.mybatis.TranslogMapper;

import io.netty.util.internal.shaded.org.jctools.queues.MpmcArrayQueue;
import org.springframework.stereotype.Repository;

@Repository
public class TranslogQueue implements TranslogShardedQueueRepository {

    private static final int QUEUE_CAPACITY = 50000;

    private final MpmcArrayQueue<TranslogEntity> sharedQueue = new MpmcArrayQueue<>(QUEUE_CAPACITY);

    @Override
    public void offer(TranslogEntity entity) {
        int retry = 0;

        while (!sharedQueue.offer(entity)) {
            if (retry++ < 10) {
                Thread.onSpinWait();
            }
        }
    }

    @Override
    public TranslogEntity poll() {
        return sharedQueue.poll();
    }

    public boolean isEmpty() {
        return sharedQueue.isEmpty();
    }
}