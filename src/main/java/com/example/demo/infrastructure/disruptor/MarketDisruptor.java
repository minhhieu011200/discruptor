package com.example.demo.infrastructure.disruptor;

import org.springframework.stereotype.Service;

import com.example.demo.application.dto.SymbolRequestDTO;
import com.example.demo.domain.service.ProcessMarketEventService;
import com.example.demo.domain.service.PublishService;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import jakarta.annotation.PreDestroy;

@Service("MarketDisruptor")
public class MarketDisruptor implements PublishService<Void, SymbolRequestDTO> {
    private final int NUM_WORKERS = 4; // số lane / worker
    private final Disruptor<MarketEvent>[] disruptors = (Disruptor<MarketEvent>[]) new Disruptor[NUM_WORKERS];
    private final ProcessMarketEventService service;
    private static final int RING_BUFFER_SIZE = 1024; // power of 2

    public MarketDisruptor(ProcessMarketEventService processMarketEvent)
            throws Exception {
        this.service = processMarketEvent;
        for (int i = 0; i < NUM_WORKERS; i++) {
            final int lane = i;
            disruptors[i] = new Disruptor<>(
                    new MarketEvent.Factory(),
                    RING_BUFFER_SIZE,
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
        int lane = (channel.hashCode() & 0x7fffffff) % NUM_WORKERS;
        long seq = disruptors[lane].getRingBuffer().next();
        try {
            MarketEvent ev = disruptors[lane].getRingBuffer().get(seq);
            ev.setEntity(e);
        } finally {
            disruptors[lane].getRingBuffer().publish(seq);
        }
        return null;
    }

    @PreDestroy
    public void shutdown() {
        for (Disruptor<MarketEvent> d : disruptors) {
            System.out.println("[MarketDisruptor] Shutting down lane...");
            d.shutdown(); // đợi event đang xử lý xong
        }
    }

}
