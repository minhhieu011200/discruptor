package com.example.demo.infrastructure.worker;

import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.repository.SymbolQueueRepository;
import com.example.demo.infrastructure.mybatis.SymbolMapper;
import com.example.demo.domain.service.PublishService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
public class SymbolWorker implements Runnable {

    private final SymbolQueueRepository queue;
    private final SymbolMapper mapper;
    private final PublishService<Void, String> redisPublishService;
    private final ObjectMapper objectMapper;

    private final int batchSize;
    private final long maxWaitMillis;

    private final ExecutorService flushExecutor;
    private final ExecutorService redisExecutor;

    private volatile boolean running = true;

    public SymbolWorker(
            SymbolQueueRepository queue,
            SymbolMapper mapper,
            PublishService<Void, String> redisPublishService,
            ObjectMapper objectMapper,
            int batchSize,
            long maxWaitMillis,
            ExecutorService flushExecutor,
            ExecutorService redisExecutor) {
        this.queue = queue;
        this.mapper = mapper;
        this.redisPublishService = redisPublishService;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
        this.maxWaitMillis = maxWaitMillis;
        this.flushExecutor = flushExecutor;
        this.redisExecutor = redisExecutor;
    }

    public void shutdown() {
        running = false;
    }

    @Override
    public void run() {

        Map<String, SymbolEntity> batchMap = new HashMap<>(batchSize);
        long firstAddTime = 0;
        long timeoutNanos = maxWaitMillis * 1_000_000;

        while (running || !queue.isEmpty() || !batchMap.isEmpty()) {
            SymbolEntity item = queue.poll();
            long now = System.nanoTime();

            if (item != null) {

                if (batchMap.isEmpty()) {
                    firstAddTime = now;
                }

                SymbolEntity old = batchMap.get(item.getImtcode());
                boolean changed = hasChanged(old, item);

                // Always override -> dedupe
                if (changed) {
                    publishRedisAsync(item);
                    batchMap.put(item.getImtcode(), item);
                }

                if (batchMap.size() >= batchSize) {
                    flushAndClear(batchMap);
                }

                continue;
            }

            // Timeout flush
            if (!batchMap.isEmpty() && now - firstAddTime >= timeoutNanos) {
                flushAndClear(batchMap);
                continue;
            }

            Thread.yield();
        }

        // Final flush before shutdown
        if (!batchMap.isEmpty()) {
            flushAndClear(batchMap);
        }
    }

    private boolean hasChanged(SymbolEntity old, SymbolEntity now) {
        if (old == null) {
            return true;
        }
        long newVersion = now.getVersion();
        long oldVersion = old.getVersion();

        if (newVersion > oldVersion) {
            return true;
        }
        return false;
    }

    private void publishRedisAsync(SymbolEntity symbol) {
        redisExecutor.submit(() -> {
            try {
                String json = objectMapper.writeValueAsString(symbol);
                redisPublishService.publish("fx-prices-channel", json);
            } catch (Exception e) {
                log.error("Redis publish error", e);
            }
        });
    }

    private void flushAndClear(Map<String, SymbolEntity> batchMap) {
        List<SymbolEntity> snapshot = new ArrayList<>(batchMap.values());
        batchMap.clear();

        try {
            flushExecutor.submit(() -> {
                try {
                    mapper.batchUpsert(snapshot);
                } catch (Exception e) {
                    log.error("[SymbolWorker flush error] {}", e.getMessage(), e);
                }
            });
        } catch (RejectedExecutionException ex) {
            log.error("[SymbolWorker flush rejected] executor overload");
        }
    }
}