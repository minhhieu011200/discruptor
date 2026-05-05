package com.example.demo.infrastructure.disruptor;

import org.slf4j.MDC;

import com.example.demo.domain.service.ProcessMarketReceiveService;
import com.lmax.disruptor.EventHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DecodeEventHandler implements EventHandler<DecodeEvent> {

    ProcessMarketReceiveService processMarketReceiveService;

    public DecodeEventHandler(ProcessMarketReceiveService processMarketReceiveService) {
        this.processMarketReceiveService = processMarketReceiveService;
    }

    @Override
    public void onEvent(DecodeEvent event, long sequence, boolean endOfBatch) throws Exception {
        this.processMarketReceiveService.process(event.data, event.getTraceId(), event.getStartTime());
    }

}
