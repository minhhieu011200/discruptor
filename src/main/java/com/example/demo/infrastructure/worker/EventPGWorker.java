package com.example.demo.infrastructure.worker;

import com.example.demo.domain.repository.EventPGQueueRepository;

public class EventPGWorker implements Runnable {

    private final EventPGQueueRepository queue;
    private final int partition;

    public EventPGWorker(EventPGQueueRepository queue, int partition) {
        this.queue = queue;
        this.partition = partition;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            Object event = queue.poll(partition);
            if (event != null) {
                handleEvent(event);
            } else {
                Thread.onSpinWait();
            }
        }
    }

    private void handleEvent(Object event) {
        System.out.println("Worker-" + partition + " xử lý: " + event);
    }
}