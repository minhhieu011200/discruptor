package com.example.demo.infrastructure.worker;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;

import com.example.demo.domain.repository.EventPGQueueRepository;

@Component
public class EventPGWorkerStarter {

    private final EventPGQueueRepository queue;
    private ExecutorService executor;

    public EventPGWorkerStarter(EventPGQueueRepository queue) {
        this.queue = queue;
    }

    @PostConstruct
    public void start() {
        int n = queue.getNumWorkers();
        executor = Executors.newFixedThreadPool(n);
        for (int i = 0; i < n; i++) {
            executor.submit(new EventPGWorker(queue, i));
        }
    }

    @PreDestroy
    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}