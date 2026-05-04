package com.example.demo.infrastructure.worker;

import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.repository.SymbolQueueRepository;
import com.example.demo.infrastructure.mybatis.SymbolMapper;
import com.example.demo.domain.service.PublishService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private volatile boolean running = true;

    public SymbolWorker(SymbolQueueRepository queue,
            SymbolMapper mapper,
            PublishService<Void, String> redisPublishService,
            ObjectMapper objectMapper,
            int batchSize,
            long maxWaitMillis,
            ExecutorService flushExecutor) {
        this.queue = queue;
        this.mapper = mapper;
        this.redisPublishService = redisPublishService;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
        this.maxWaitMillis = maxWaitMillis;
        this.flushExecutor = flushExecutor;
    }

    public void shutdown() {
        running = false;
    }

    @Override
    public void run() {
        Map<String, SymbolEntity> batchMap = new HashMap<>(batchSize);
        long firstAddTime = 0;
        long timeoutNanos = maxWaitMillis * 1_000_000L;

        while (running || !queue.isEmpty() || !batchMap.isEmpty()) {
            SymbolEntity item = queue.poll();
            long now = System.nanoTime();

            if (item != null) {
                if (batchMap.isEmpty()) {
                    firstAddTime = now;
                }

                SymbolEntity cloned = cloneSymbol(item);
                batchMap.put(cloned.getImtcode(), cloned);

                try {
                    String json = objectMapper.writeValueAsString(cloned);
                    redisPublishService.publish("fx-prices-channel", json);
                } catch (Exception e) {
                    log.error("Failed to serialize symbol for redis", e);
                }

                if (batchMap.size() >= batchSize) {
                    flush(new ArrayList<>(batchMap.values()));
                    batchMap.clear();
                }
                continue;
            }

            if (!batchMap.isEmpty() && timeoutNanos > 0 && now - firstAddTime >= timeoutNanos) {
                flush(new ArrayList<>(batchMap.values()));
                batchMap.clear();
                continue;
            }

            Thread.yield();
        }

        if (!batchMap.isEmpty()) {
            flush(new ArrayList<>(batchMap.values()));
        }
    }

    private SymbolEntity cloneSymbol(SymbolEntity s) {
        SymbolEntity copy = new SymbolEntity();
        copy.setImtcode(s.getImtcode());
        copy.setBuyCurrency(s.getBuyCurrency());
        copy.setSellCurrency(s.getSellCurrency());
        copy.setBid(s.getBid());
        copy.setAsk(s.getAsk());
        copy.setTenor(s.getTenor());
        copy.setStatus(s.getStatus());
        copy.setSpread(s.getSpread());
        return copy;
    }

    private void flush(List<SymbolEntity> batch) {
        try {
            flushExecutor.submit(() -> {
                try {
                    mapper.batchUpsert(batch);
                } catch (Exception e) {
                    log.error("[SymbolWorker flush error] " + e.getMessage(), e);
                }
            });
        } catch (RejectedExecutionException ex) {
            log.error("[SymbolWorker flush rejected] executor overload");
        }
    }
}
