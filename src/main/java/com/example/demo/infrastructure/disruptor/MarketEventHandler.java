package com.example.demo.infrastructure.disruptor;

import java.util.concurrent.TimeUnit;

import com.example.demo.domain.service.ProcessMarketEventService;
import com.lmax.disruptor.EventHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MarketEventHandler implements EventHandler<MarketEvent> {
    private ProcessMarketEventService processMarketEventService;

    public MarketEventHandler(int lane, ProcessMarketEventService processMarketEventService) {
        this.processMarketEventService = processMarketEventService;
    }

    @Override
    public void onEvent(MarketEvent event, long sequence, boolean endOfBatch) throws Exception {
        processMarketEventService.process(event.getEntity());
        String traceId = event.getEntity().getTraceId();
        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - event.getEntity().getStartTime());
        log.info("Finished processing event traceId={} in {}ms", traceId, duration);

    }
}
