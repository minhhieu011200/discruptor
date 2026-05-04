package com.example.demo.infrastructure.disruptor;

import org.slf4j.MDC;

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
        if (event.getEntity().getTraceId() != null) {
            MDC.put("traceId", event.getEntity().getTraceId());
        }
        if (event.getEntity().getStartTime() > 0) {
            MDC.put("startTime", String.valueOf(event.getEntity().getStartTime()));
        }
        try {
            processMarketEventService.process(event.getEntity());
        } finally {
            if (event.getEntity().getStartTime() > 0) {
                long duration = System.currentTimeMillis() - event.getEntity().getStartTime();
                log.info("Finished processing event imt={} traceId={} in {}ms", event.getEntity().getImtcode(),
                        event.getEntity().getTraceId(), duration);
            }
            MDC.remove("traceId");
            MDC.remove("startTime");
        }
    }
}
