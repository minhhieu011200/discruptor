package com.example.demo.infrastructure.disruptor;

import org.slf4j.MDC;

import com.example.demo.domain.service.ProcessMarketReceiveService;
import com.lmax.disruptor.EventHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DecodeEventHandler implements EventHandler<DecodeEvent> {

    ProcessMarketReceiveService<byte[]> processMarketReceiveService;

    public DecodeEventHandler(ProcessMarketReceiveService<byte[]> processMarketReceiveService) {
        this.processMarketReceiveService = processMarketReceiveService;
    }

    @Override
    public void onEvent(DecodeEvent event, long sequence, boolean endOfBatch) throws Exception {
        if (event.traceId != null) {
            MDC.put("traceId", event.traceId);
        }
        if (event.startTime > 0) {
            MDC.put("startTime", String.valueOf(event.startTime));
        }
        try {
            this.processMarketReceiveService.process(event.data);
        } finally {
            MDC.remove("traceId");
            MDC.remove("startTime");
        }
    }

}
