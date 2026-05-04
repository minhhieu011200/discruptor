package com.example.demo.domain.repository;

import com.example.demo.domain.entity.SymbolEntity;

public interface SymbolQueueRepository {
    void offer(SymbolEntity entity);
    SymbolEntity poll();
    boolean isEmpty();
}
