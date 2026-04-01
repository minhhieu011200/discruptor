package com.example.demo.infrastructure.disruptor;

import com.example.demo.domain.service.ProcessMarketReceiveService;
import com.example.demo.infrastructure.metrics.MessageThroughputMetrics;
import com.lmax.disruptor.EventHandler;

public class DecodeEventHandler implements EventHandler<DecodeEvent> {

    ProcessMarketReceiveService<byte[]> processMarketReceiveService;
    private final MessageThroughputMetrics metrics;

    public DecodeEventHandler(ProcessMarketReceiveService<byte[]> processMarketReceiveService) {
        this(processMarketReceiveService, null);
    }

    public DecodeEventHandler(ProcessMarketReceiveService<byte[]> processMarketReceiveService,
                              MessageThroughputMetrics metrics) {
        this.processMarketReceiveService = processMarketReceiveService;
        this.metrics = metrics;
    }

    @Override
    public void onEvent(DecodeEvent event, long sequence, boolean endOfBatch) throws Exception {
        this.processMarketReceiveService.process(event.getData());
        if (this.metrics != null) {
            try {
                this.metrics.recordMessages(1, "disruptor_decode");
            } catch (Exception ignored) {
            }
        }
    }

}
