package com.example.demo.infrastructure.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;

import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.repository.SymbolRepository;
import com.example.demo.infrastructure.mybatis.SymbolMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

public class SymbolWorker {
    private static final int BATCH_SIZE = 5000;

    @Value("${symbol.worker.count:4}")
    private final int workerCount = 4;

    @Value("${symbol.flush.interval.ms:500}")
    private final int flushIntervalMs = 500;

    private final SymbolMapper mapper;
    private final SymbolRepository symbolRepository;

    private volatile boolean running = true;
    private Thread flushThread;

    // WorkerPool có bounded queue → tránh OOM
    private ExecutorService workerPool;

    public SymbolWorker(SymbolMapper mapper, SymbolRepository symbolRepository) {
        this.mapper = mapper;
        this.symbolRepository = symbolRepository;
        // initialize workerPool using configured workerCount
        this.workerPool = new ThreadPoolExecutor(
                workerCount,
                workerCount,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(workerCount * 2),
                new ThreadPoolExecutor.DiscardPolicy());
    }

    // -------------------------------
    // STARTUP
    // -------------------------------
    @PostConstruct
    public void init() {
        flushThread = new Thread(this::flushLoop, "symbol-flush-thread");
        flushThread.setDaemon(true);
        flushThread.start();
        System.out.println("[SymbolWorker] Started");
    }

    // -------------------------------
    // MAIN FLUSH LOOP
    // -------------------------------
    private void flushLoop() {
        while (running) {
            try {
                flushDirtySymbols();
                Thread.sleep(flushIntervalMs);
            } catch (InterruptedException e) {
                // nếu running = false → break
                if (!running)
                    break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("[SymbolWorker] flushLoop stopped");
    }

    // -------------------------------
    // FLUSHING LOGIC
    // -------------------------------
    private void flushDirtySymbols() {

        Map<String, SymbolEntity> map = symbolRepository.getAll();

        List<SymbolEntity> buffer = new ArrayList<>(BATCH_SIZE);

        for (SymbolEntity s : map.values()) {

            if (!s.isDirty())
                continue;

            buffer.add((SymbolEntity) s.clone());
            s.markClean();

            if (buffer.size() >= BATCH_SIZE) {
                submitBatch(new ArrayList<>(buffer));
                buffer.clear();
            }
        }

        if (!buffer.isEmpty())
            submitBatch(buffer);
    }

    private void submitBatch(List<SymbolEntity> batch) {
        try {
            workerPool.submit(() -> {
                try {
                    mapper.batchUpsert(batch);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (RejectedExecutionException ignored) {
            // Queue full → bỏ batch tránh OOM
        }
    }

    // -------------------------------
    // GRACEFUL SHUTDOWN
    // -------------------------------
    @PreDestroy
    public void shutdown() {
        System.out.println("[SymbolWorker] Shutdown requested...");

        // 1. Stop flush thread
        running = false;
        flushThread.interrupt();

        // 2. Shutdown workerPool
        workerPool.shutdown();

        try {
            if (!workerPool.awaitTermination(10, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        try {
            flushDirtySymbols();
        } catch (Exception ignored) {
        }

    }
}
