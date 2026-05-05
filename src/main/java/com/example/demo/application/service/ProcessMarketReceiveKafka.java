package com.example.demo.application.service;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.example.demo.application.dto.SymbolRequestDTO;
import com.example.demo.domain.service.ProcessMarketParseService;
import com.example.demo.domain.service.ProcessMarketReceiveService;
import com.example.demo.domain.service.PublishMarketDisruptor;

import com.example.demo.application.annotation.Measured;
import com.example.demo.application.annotation.TraceLog;

@Service
public class ProcessMarketReceiveKafka implements ProcessMarketReceiveService {
    private ProcessMarketParseService processMarketParse;
    private PublishMarketDisruptor publishService;

    public ProcessMarketReceiveKafka(ProcessMarketParseService processMarketParse,
            PublishMarketDisruptor publishService) {
        this.processMarketParse = processMarketParse;
        this.publishService = publishService;
    }

    @Override
    @Measured(value = "kafka.receive.process", description = "Time to process message received from Kafka and publish to disruptor")
    @TraceLog("ProcessMarketReceiveKafka")
    public void process(byte[] data, String traceId, Long startTime) {
        SymbolRequestDTO me = processMarketParse.process(data, traceId, startTime);
        if (me.getImtcode() == null) {
            return;
        }
        publishService.publishTrace(me.getImtcode(), me, traceId, startTime);
    }

}
