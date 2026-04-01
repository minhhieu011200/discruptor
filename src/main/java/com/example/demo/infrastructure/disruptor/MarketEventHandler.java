package com.example.demo.infrastructure.disruptor;

import java.util.concurrent.atomic.AtomicLong;

import com.example.demo.domain.service.ProcessMarketEventService;
import com.example.demo.infrastructure.metrics.MessageThroughputMetrics;
import com.lmax.disruptor.EventHandler;

public class MarketEventHandler implements EventHandler<MarketEvent> {
    private ProcessMarketEventService processMarketEventService;
    private final MessageThroughputMetrics metrics;

    public MarketEventHandler(int lane, AtomicLong counter, ProcessMarketEventService processMarketEventService) {
        this(lane, processMarketEventService, null);
    }

    public MarketEventHandler(int lane, ProcessMarketEventService processMarketEventService) {
        this(lane, processMarketEventService, null);
    }

    public MarketEventHandler(int lane, ProcessMarketEventService processMarketEventService,
            MessageThroughputMetrics metrics) {
        this.processMarketEventService = processMarketEventService;
        this.metrics = metrics;
    }

    @Override
    public void onEvent(MarketEvent event, long sequence, boolean endOfBatch) throws Exception {
        processMarketEventService.process(event.getEntity());
        if (this.metrics != null) {
            try {
                this.metrics.recordMessages(1, "market_event_handler");
            } catch (Exception ignored) {
            }
        }

    }
}
