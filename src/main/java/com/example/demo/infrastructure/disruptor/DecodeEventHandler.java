package com.example.demo.infrastructure.disruptor;

import com.example.demo.domain.service.ProcessMarketReceiveService;
import com.lmax.disruptor.EventHandler;

public class DecodeEventHandler implements EventHandler<DecodeEvent> {

    ProcessMarketReceiveService<byte[]> processMarketReceiveService;

    public DecodeEventHandler(ProcessMarketReceiveService<byte[]> processMarketReceiveService) {
        this.processMarketReceiveService = processMarketReceiveService;
    }

    @Override
    public void onEvent(DecodeEvent event, long sequence, boolean endOfBatch) throws Exception {
        this.processMarketReceiveService.process(event.getData());
    }

}
