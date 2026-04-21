package com.example.demo.infrastructure.worker;

import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.repository.EventPGQueueRepository;

@Repository
public class EventPGQueue implements EventPGQueueRepository {
    private final int numWorkers;

    private final ManyToOneConcurrentArrayQueue<Object>[] queues;

    public EventPGQueue(@Value("${event-pg.worker.count:4}") int numWorkers) {
        this.numWorkers = numWorkers;
        this.queues = new ManyToOneConcurrentArrayQueue[numWorkers];
        for (int i = 0; i < numWorkers; i++) {
            queues[i] = new ManyToOneConcurrentArrayQueue<>(5000);
        }
    }

    public int getNumWorkers() {
        return numWorkers;
    }

    private int getPartition(String channel) {
        return Math.abs(channel.hashCode()) % numWorkers;
    }

    public void offer(String channel, Object event) {
        int partition = getPartition(channel);
        ManyToOneConcurrentArrayQueue<Object> queue = queues[partition];

        while (!queue.offer(event)) {
            Thread.yield();
        }
    }

    public Object poll(int partition) {
        return queues[partition].poll();
    }

}
