package com.example.demo.domain.repository;

public interface EventPGQueueRepository {
    void offer(String channel, Object event);

    Object poll(int partition);

    int getNumWorkers();
}