package com.example.demo.infrastructure.disruptor;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.service.ProcessMarketEventService;
import com.example.demo.domain.service.PublishService;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

@Service
public class MarketDisruptor implements PublishService<Void, SymbolEntity> {
    private final int NUM_WORKERS = 16; // số lane / worker
    private final Disruptor<MarketEvent>[] disruptors = (Disruptor<MarketEvent>[]) new Disruptor[NUM_WORKERS];
    private final ProcessMarketEventService service;

    public MarketDisruptor(ProcessMarketEventService processMarketEvent) throws Exception {
        this.service = processMarketEvent;
        for (int i = 0; i < NUM_WORKERS; i++) {
            final int lane = i;
            disruptors[i] = new Disruptor<>(
                    new MarketEvent.Factory(),
                    1024,
                    Thread.ofPlatform().name("lane-" + lane + "-").factory(),
                    ProducerType.SINGLE,
                    new BusySpinWaitStrategy());

            disruptors[i].handleEventsWith(new MarketEventHandler(lane, service));

            disruptors[i].start();

        }
    }

    // Publisher gọi: hash symbolId → lane
    public Void publish(String channel, SymbolEntity e) {
        int lane = (int) (channel.hashCode() % NUM_WORKERS);
        long seq = disruptors[lane].getRingBuffer().next();
        try {
            MarketEvent ev = disruptors[lane].getRingBuffer().get(seq);
            ev.setEntity(e);
        } finally {
            disruptors[lane].getRingBuffer().publish(seq);
        }
        return null;
    }

}
