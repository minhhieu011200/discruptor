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
    private SymbolWorker worker;
    private ExecutorService flushExecutor;

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
        workerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("symbol-worker");
            t.setDaemon(true);
            return t;
        });

        flushExecutor = Executors.newFixedThreadPool(workerCount, r -> {
            Thread t = new Thread(r);
            t.setName("symbol-flush-" + t.getId());
            t.setDaemon(true);
            return t;
        });

        worker = new SymbolWorker(
                queue,
                mapper,
                redisPublishService,
                objectMapper,
                1000,
                flushMillis,
                flushExecutor
        );

        workerExecutor.submit(worker);
        log.info("[SymbolWorkerStarter] Started worker (Executor)");
    }

    @PreDestroy
    public void stop() {
        log.info("[SymbolWorkerStarter] Stopping...");
        if (worker != null) {
            worker.shutdown();
        }
        if (workerExecutor != null) {
            workerExecutor.shutdown();
            try {
                if (!workerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    workerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                workerExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (flushExecutor != null) {
            flushExecutor.shutdown();
            try {
                if (!flushExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    flushExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                flushExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("[SymbolWorkerStarter] Stopped cleanly");
    }
}
