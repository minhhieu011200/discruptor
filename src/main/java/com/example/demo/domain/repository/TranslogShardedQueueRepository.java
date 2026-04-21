package com.example.demo.domain.repository;

import com.example.demo.domain.entity.TranslogEntity;

public interface TranslogShardedQueueRepository {
    void offer(TranslogEntity entity);

    TranslogEntity poll(int partition);

    boolean isEmpty();
}
