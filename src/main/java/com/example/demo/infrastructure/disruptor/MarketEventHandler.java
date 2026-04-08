package com.example.demo.infrastructure.disruptor;

import com.example.demo.domain.service.ProcessMarketEventService;
import com.lmax.disruptor.EventHandler;

public class MarketEventHandler implements EventHandler<MarketEvent> {
    private ProcessMarketEventService processMarketEventService;

    public MarketEventHandler(int lane, ProcessMarketEventService processMarketEventService) {
        this.processMarketEventService = processMarketEventService;
    }

    @Override
    public void onEvent(MarketEvent event, long sequence, boolean endOfBatch) throws Exception {
        processMarketEventService.process(event.getEntity());

    }
}
