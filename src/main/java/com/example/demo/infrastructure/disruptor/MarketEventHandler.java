package com.example.demo.infrastructure.disruptor;

import java.util.concurrent.TimeUnit;

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
        processMarketEventService.process(event.getEntity());
        if (event.getEntity().getStartTime() > 0) {
            long duration = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - event.getEntity().getStartTime());
            log.info("Finished processing event traceId={} in {}us", event.getEntity().getTraceId(), duration);
        }
        MDC.remove("traceId");
        MDC.remove("startTime");

    }
}
