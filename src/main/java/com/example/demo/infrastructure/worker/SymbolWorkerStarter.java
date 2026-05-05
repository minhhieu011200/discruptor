package com.example.demo.infrastructure.worker;

import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.repository.SymbolQueueRepository;
import com.example.demo.domain.service.PublishService;
import com.example.demo.infrastructure.mybatis.SymbolMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class SymbolWorkerStarter {

    private final SymbolQueueRepository queue;
    private final SymbolMapper mapper;
    private final PublishService<Void, String> redisPublishService;
    private final ObjectMapper objectMapper;
    private final long flushMillis;
    private final int workerCount;

    private ExecutorService workerExecutor;
    private ExecutorService flushExecutor;
    private ExecutorService redisExecutor;

    private SymbolWorker worker;

    public SymbolWorkerStarter(
            SymbolQueueRepository queue,
            SymbolMapper mapper,
            @Qualifier("RedisPubSub") PublishService<Void, String> redisPublishService,
            ObjectMapper objectMapper,
            @Value("${symbol.flush.millis:500}") long flushMillis,
            @Value("${symbol.worker.count:2}") int workerCount) {
        this.queue = queue;
        this.mapper = mapper;
        this.redisPublishService = redisPublishService;
        this.objectMapper = objectMapper;
        this.flushMillis = flushMillis;
        this.workerCount = workerCount;
    }

    @PostConstruct
    public void start() {

        // ===== MAIN WORKER =====
        workerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("symbol-worker");
            t.setDaemon(true);
            return t;
        });

        // ===== DB EXECUTOR =====
        flushExecutor = Executors.newFixedThreadPool(workerCount, r -> {
            Thread t = new Thread(r);
            t.setName("symbol-flush-" + t.getId());
            t.setDaemon(true);
            return t;
        });

        // ===== REDIS EXECUTOR =====
        redisExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r);
            t.setName("symbol-redis-" + t.getId());
            t.setDaemon(true);
            return t;
        });

        // ===== WORKER INSTANCE =====
        worker = new SymbolWorker(
                queue,
                mapper,
                redisPublishService,
                objectMapper,
                1000,
                flushMillis,
                flushExecutor,
                redisExecutor // <<< thêm vào đây
        );

        workerExecutor.submit(worker);
        log.info("[SymbolWorkerStarter] Started worker with redisExecutor + flushExecutor");
    }

    @PreDestroy
    public void stop() {
        log.info("[SymbolWorkerStarter] Stopping...");

        if (worker != null) {
            worker.shutdown();
        }

        shutdownExecutor(workerExecutor, 5, "workerExecutor");
        shutdownExecutor(flushExecutor, 10, "flushExecutor");
        shutdownExecutor(redisExecutor, 5, "redisExecutor");

        log.info("[SymbolWorkerStarter] Stopped cleanly");
    }

    private void shutdownExecutor(ExecutorService executor, int timeoutSeconds, String name) {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}