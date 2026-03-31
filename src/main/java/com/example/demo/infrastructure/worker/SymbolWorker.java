package com.example.demo.infrastructure.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.repository.SymbolRepository;
import com.example.demo.infrastructure.mybatis.SymbolMapper;

import jakarta.annotation.PostConstruct;

public class SymbolWorker {
    private static final int BATCH_SIZE = 5000;
    private static final int WORKER_COUNT = 8;
    private static final int FLUSH_INTERVAL_MS = 500;

    private final SymbolMapper mapper;
    private final SymbolRepository symbolRepository;

    // WorkerPool có bounded queue → tránh OOM
    private final ExecutorService workerPool = new ThreadPoolExecutor(
            WORKER_COUNT, WORKER_COUNT,
            0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(WORKER_COUNT * 2), // bounded
            new ThreadPoolExecutor.DiscardPolicy() // nếu quá tải → bỏ batch
    );

    public SymbolWorker(SymbolMapper mapper, SymbolRepository symbolRepository) {
        this.mapper = mapper;
        this.symbolRepository = symbolRepository;
    }

    @PostConstruct
    public void init() {
        Thread flushThread = new Thread(this::flushLoop);
        flushThread.setDaemon(true);
        flushThread.start();
    }

    private void flushLoop() {
        while (true) {
            try {
                flushDirtySymbols();
                Thread.sleep(FLUSH_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void flushDirtySymbols() {
        // Lấy map từ ChronicleMap
        Map<String, SymbolEntity> map = symbolRepository.getAll();

        List<SymbolEntity> buffer = new ArrayList<>(BATCH_SIZE);

        for (SymbolEntity s : map.values()) {
            if (!s.isDirty())
                continue; // chỉ flush phần thay đổi (tối ưu quan trọng)

            buffer.add((SymbolEntity) s.clone()); // clone → tránh mutation khi worker xử lý
            s.markClean(); // reset dirty

            if (buffer.size() >= BATCH_SIZE) {
                submitBatch(new ArrayList<>(buffer));
                buffer.clear();
            }
        }

        if (!buffer.isEmpty()) {
            submitBatch(buffer);
        }
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
            // Queue full → bỏ batch, tránh OOM
        }
    }
}
