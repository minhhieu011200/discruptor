package com.example.demo.infrastructure.worker;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;

import com.example.demo.application.service.pgEvent.ProcessPgEventStrategy;
import com.example.demo.domain.repository.EventPGQueueRepository;

@Component
public class EventPGWorkerStarter {

    private final EventPGQueueRepository queue;
    private final ProcessPgEventStrategy strategy;
    private ExecutorService executor;

    public EventPGWorkerStarter(EventPGQueueRepository queue, ProcessPgEventStrategy strategy) {
        this.queue = queue;
        this.strategy = strategy;
    }

    @PostConstruct
    public void start() {
        int n = queue.getNumWorkers();
        executor = Executors.newFixedThreadPool(n);
        for (int i = 0; i < n; i++) {
            executor.submit(new EventPGWorker(queue, i, strategy));
        }
    }

    @PreDestroy
    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}