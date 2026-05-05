package com.example.demo.infrastructure.worker;

import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.repository.SymbolQueueRepository;
import io.netty.util.internal.shaded.org.jctools.queues.MpmcArrayQueue;
import org.springframework.stereotype.Repository;

@Repository
public class SymbolQueue implements SymbolQueueRepository {

    private static final int QUEUE_CAPACITY = 50000;
    private final MpmcArrayQueue<SymbolEntity> sharedQueueSymbol = new MpmcArrayQueue<>(QUEUE_CAPACITY);

    @Override
    public void offer(SymbolEntity entity) {
        int retry = 0;
        while (!sharedQueueSymbol.offer(entity)) {
            if (retry++ < 10) {
                Thread.onSpinWait();
            } else {
                break;
            }
        }
    }

    @Override
    public SymbolEntity poll() {
        return sharedQueueSymbol.poll();
    }

    @Override
    public boolean isEmpty() {
        return sharedQueueSymbol.isEmpty();
    }
}
