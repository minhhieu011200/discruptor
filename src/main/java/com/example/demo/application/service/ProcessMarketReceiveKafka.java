package com.example.demo.application.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.example.demo.application.dto.SymbolRequestDTO;
import com.example.demo.domain.service.ProcessMarketParseService;
import com.example.demo.domain.service.ProcessMarketReceiveService;
import com.example.demo.domain.service.PublishService;
import com.example.demo.application.annotation.Measured;
import com.example.demo.application.annotation.TraceLog;

@Service
public class ProcessMarketReceiveKafka implements ProcessMarketReceiveService<byte[]> {
    private ProcessMarketParseService processMarketParse;
    private PublishService<Void, SymbolRequestDTO> publishService;

    public ProcessMarketReceiveKafka(ProcessMarketParseService processMarketParse,
            @Qualifier("MarketDisruptor") PublishService<Void, SymbolRequestDTO> publishService) {
        this.processMarketParse = processMarketParse;
        this.publishService = publishService;
    }

    @Override
    @Measured(value = "kafka.receive.process", description = "Time to process message received from Kafka and publish to disruptor")
    @TraceLog("ProcessMarketReceiveKafka")
    public void process(byte[] data) {
        SymbolRequestDTO me = processMarketParse.process(data);
        if (me.getImtcode() == null) {
            return;
        }
        me.setTraceId(org.slf4j.MDC.get("traceId"));

        String st = org.slf4j.MDC.get("startTime");
        if (st != null)
            me.setStartTime(Long.parseLong(st));

        publishService.publish(me.getImtcode(), me);
    }

}
