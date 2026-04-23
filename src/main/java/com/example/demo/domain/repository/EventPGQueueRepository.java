package com.example.demo.domain.repository;

import com.fasterxml.jackson.databind.JsonNode;

public interface EventPGQueueRepository {
    void offer(String channel, JsonNode event);

    JsonNode poll(int partition);

    int getNumWorkers();
}