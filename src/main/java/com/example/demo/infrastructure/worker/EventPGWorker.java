package com.example.demo.infrastructure.worker;

import com.example.demo.application.service.pgEvent.ProcessPgEventStrategy;
import com.example.demo.domain.repository.EventPGQueueRepository;
import com.example.demo.domain.service.ProcessPgEventService;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventPGWorker implements Runnable {

    private final EventPGQueueRepository queue;
    private final int partition;

    private final ProcessPgEventStrategy strategy;

    public EventPGWorker(EventPGQueueRepository queue, int partition, ProcessPgEventStrategy strategy) {
        this.queue = queue;
        this.partition = partition;
        this.strategy = strategy;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            JsonNode event = queue.poll(partition);
            if (event != null) {
                handleEvent(event);
            } else {
                Thread.onSpinWait();
            }
        }
    }

    private void handleEvent(JsonNode event) {
        String datatype = event.path("datatype").asText();
        ProcessPgEventService handler = strategy.getHandler(datatype);

        if (handler == null) {
            log.warn("Không có handler cho datatype: {}", datatype);
            return;
        }
        handler.process(event);
    }
}