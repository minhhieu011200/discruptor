package com.example.demo.infrastructure.disruptor;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.example.demo.application.dto.SymbolRequestDTO;
import com.example.demo.domain.service.ProcessMarketEventService;
import com.example.demo.domain.service.PublishMarketDisruptor;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Service("MarketDisruptor")
@Slf4j
public class MarketDisruptor implements PublishMarketDisruptor {
    private final int numWorkers; // số lane / worker
    private final Disruptor<MarketEvent>[] disruptors;
    private final ProcessMarketEventService service;
    private final int ringBufferSize; // power of 2

    public MarketDisruptor(ProcessMarketEventService processMarketEvent,
            @Value("${market.disruptor.num.workers:4}") int numWorkers,
            @Value("${market.ring.buffer.size:1024}") int ringBufferSize)
            throws Exception {
        this.service = processMarketEvent;
        this.numWorkers = numWorkers;
        this.ringBufferSize = ringBufferSize;
        this.disruptors = (Disruptor<MarketEvent>[]) new Disruptor[numWorkers];

        for (int i = 0; i < numWorkers; i++) {
            final int lane = i;
            disruptors[i] = new Disruptor<>(
                    new MarketEvent.Factory(),
                    this.ringBufferSize,
                    Thread.ofPlatform().name("lane-" + lane + "-").factory(),
                    ProducerType.SINGLE,
                    new BusySpinWaitStrategy());

            disruptors[i].handleEventsWith(new MarketEventHandler(lane, service));

            disruptors[i].start();

        }
    }

    // Publisher gọi: hash symbolId → lane
    @Override
    public Void publish(String channel, SymbolRequestDTO e) {
        int lane = (channel.hashCode() & 0x7fffffff) % numWorkers;
        long seq = disruptors[lane].getRingBuffer().next();
        try {
            MarketEvent ev = disruptors[lane].getRingBuffer().get(seq);
            ev.setEntity(e);
        } finally {
            disruptors[lane].getRingBuffer().publish(seq);
        }
        return null;
    }

    @Override
    public Void publishTrace(String channel, SymbolRequestDTO e, String traceId, long startTime) {
        int lane = (channel.hashCode() & 0x7fffffff) % numWorkers;
        long seq = disruptors[lane].getRingBuffer().next();
        try {
            MarketEvent ev = disruptors[lane].getRingBuffer().get(seq);
            ev.setEntity(e);
            ev.getEntity().setTraceId(traceId);
            ev.getEntity().setStartTime(startTime);
        } finally {
            disruptors[lane].getRingBuffer().publish(seq);
        }
        return null;
    }

    @PreDestroy
    public void shutdown() {
        for (Disruptor<MarketEvent> d : disruptors) {
            log.info("[MarketDisruptor] Shutting down lane...");
            d.shutdown(); // đợi event đang xử lý xong
        }
    }

}
