package com.example.demo.infrastructure.worker;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.demo.infrastructure.mybatis.TranslogMapper;

@Component
public class TranslogWorkerStarter {

    private final TranslogQueue queue;
    private final TranslogMapper mapper;

    private final int workerCount;
    private final long flushMillis;

    private ExecutorService executor;
    private TransLogWorker[] workers;

    public TranslogWorkerStarter(
            TranslogQueue queue,
            TranslogMapper mapper,
            @Value("${translog.worker.count:4}") int workerCount,
            @Value("${translog.flush.millis:1000}") long flushMillis) {

        this.queue = queue;
        this.mapper = mapper;
        this.workerCount = workerCount;
        this.flushMillis = flushMillis;
    }

    @PostConstruct
    public void start() {
        executor = Executors.newFixedThreadPool(workerCount, r -> {
            Thread t = new Thread(r);
            t.setName("translog-worker-" + t.getId());
            return t;
        });

        workers = new TransLogWorker[workerCount];

        for (int i = 0; i < workerCount; i++) {
            TransLogWorker worker = new TransLogWorker(queue, i, mapper, 5000, flushMillis);

            workers[i] = worker;
            executor.submit(worker);
        }

        System.out.println("[TranslogWorkerStarter] Started " + workerCount + " workers");
    }

    @PreDestroy
    public void stop() {
        System.out.println("[TranslogWorkerStarter] Stopping...");

        for (TransLogWorker w : workers) {
            w.shutdown();
        }

        executor.shutdown();

        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("[TranslogWorkerStarter] Stopped cleanly");
    }
}