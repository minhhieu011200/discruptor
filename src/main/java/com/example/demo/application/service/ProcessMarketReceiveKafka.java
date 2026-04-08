package com.example.demo.application.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.example.demo.application.dto.SymbolRequestDTO;
import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.service.ProcessMarketParseService;
import com.example.demo.domain.service.ProcessMarketReceiveService;
import com.example.demo.domain.service.PublishService;
import com.example.demo.application.annotation.Measured;

@Service
public class ProcessMarketReceiveKafka implements ProcessMarketReceiveService<byte[]> {
    private ProcessMarketParseService processMarketParseSocket;
    private PublishService<Void, SymbolRequestDTO> publishService;

    public ProcessMarketReceiveKafka(ProcessMarketParseService processMarketParseSocket,
            @Qualifier("MarketDisruptor") PublishService<Void, SymbolRequestDTO> publishService) {
        this.processMarketParseSocket = processMarketParseSocket;
        this.publishService = publishService;
    }

    @Override
    @Measured(value = "kafka.receive.process", description = "Time to process message received from Kafka and publish to disruptor")
    public void process(byte[] data) {
        SymbolRequestDTO me = processMarketParseSocket.process(data);
        if (me.getImtcode() == null) {
            return;
        }
        publishService.publish(me.getImtcode(), me);
    }

}
