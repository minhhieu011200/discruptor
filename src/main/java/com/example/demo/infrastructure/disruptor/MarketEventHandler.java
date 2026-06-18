package com.example.demo.infrastructure.disruptor;

import java.util.concurrent.TimeUnit;

import com.example.demo.domain.service.ProcessMarketEventService;
import com.lmax.disruptor.EventHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MarketEventHandler implements EventHandler<MarketEvent> {
    private final ProcessMarketEventService processMarketEventService;

    public MarketEventHandler(int lane, ProcessMarketEventService processMarketEventService) {
        this.processMarketEventService = processMarketEventService;
    }

    @Override
    public void onEvent(MarketEvent event, long sequence, boolean endOfBatch) throws Exception {
        processMarketEventService.process(event.getEntity());
        if (log.isTraceEnabled()) {
            long duration = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - event.getEntity().getStartTime());
            log.trace("[lane] traceId={} latency={}µs", event.getEntity().getTraceId(), duration);
        }
    }
}
