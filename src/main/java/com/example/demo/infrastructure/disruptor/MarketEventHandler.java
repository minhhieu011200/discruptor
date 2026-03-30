package com.example.demo.infrastructure.disruptor;

import java.util.concurrent.atomic.AtomicLong;

import com.example.demo.domain.service.ProcessMarketEventService;
import com.lmax.disruptor.EventHandler;

public class MarketEventHandler implements EventHandler<MarketEvent> {
    private final int lane;
    private ProcessMarketEventService processMarketEventService;

    public MarketEventHandler(int lane, AtomicLong counter, ProcessMarketEventService processMarketEventService) {
        this(lane, processMarketEventService);
    }

    public MarketEventHandler(int lane, ProcessMarketEventService processMarketEventService) {
        this.lane = lane;
        this.processMarketEventService = processMarketEventService;
    }

    @Override
    public void onEvent(MarketEvent event, long sequence, boolean endOfBatch) throws Exception {
        processMarketEventService.process(event.getEntity());
    }
}
