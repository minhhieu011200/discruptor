package com.example.demo.infrastructure.worker;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.demo.infrastructure.mybatis.TranslogMapper;

@Component
@Slf4j
public class TranslogWorkerStarter {

    private final TranslogQueue queue;
    private final TranslogMapper mapper;
    private final long flushMillis;
    private final int workerCount;

    private ExecutorService workerExecutor;
    private TransLogWorker worker;

    private ExecutorService flushExecutor;

    public TranslogWorkerStarter(
            TranslogQueue queue,
            TranslogMapper mapper,
            @Value("${translog.flush.millis:50}") long flushMillis,
            @Value("${translog.worker.count:4}") int workerCount) {

        this.queue = queue;
        this.mapper = mapper;
        this.flushMillis = flushMillis;
        this.workerCount = workerCount;
    }

    @PostConstruct
    public void start() {

        // 1. Worker executor (1 thread)
        workerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("translog-worker");
            t.setDaemon(true);
            return t;
        });

        // 2. Flush executor (DB writer pool)
        flushExecutor = Executors.newFixedThreadPool(workerCount, r -> {
            Thread t = new Thread(r);
            t.setName("translog-flush-" + t.getId());
            t.setDaemon(true);
            return t;
        });

        // 3. Inject flushExecutor vào worker
        worker = new TransLogWorker(
                queue,
                mapper,
                5000,
                flushMillis,
                flushExecutor // 🔥 QUAN TRỌNG
        );

        workerExecutor.submit(worker);

        log.info("[TranslogWorkerStarter] Started worker (Executor)");
    }

    @PreDestroy
    public void stop() {
        log.info("[TranslogWorkerStarter] Stopping...");

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

        // 🔥 QUAN TRỌNG: shutdown flushExecutor
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

        log.info("[TranslogWorkerStarter] Stopped cleanly");
    }
}