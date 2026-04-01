package com.example.demo.application.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.service.ProcessMarketParseService;
import com.example.demo.domain.service.ProcessMarketReceiveService;
import com.example.demo.domain.service.PublishService;

@Service
public class ProcessMarketReceiveKafka implements ProcessMarketReceiveService<byte[]> {
    private ProcessMarketParseService processMarketParseSocket;
    private PublishService<Void, SymbolEntity> publishService;

    public ProcessMarketReceiveKafka(ProcessMarketParseService processMarketParseSocket,
            @Qualifier("MarketDisruptor") PublishService<Void, SymbolEntity> publishService) {
        this.processMarketParseSocket = processMarketParseSocket;
        this.publishService = publishService;
    }

    @Override
    public void process(byte[] data) {
        SymbolEntity me = processMarketParseSocket.process(data);
        publishService.publish(me.getImtcode(), me);
    }

}
