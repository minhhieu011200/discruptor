package com.example.demo.application.service;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.service.ProcessMarketParseService;
import com.example.demo.domain.service.ProcessMarketReceiveService;
import com.example.demo.domain.service.PublishService;

@Service
public class ProcessMarketReceiveSocket implements ProcessMarketReceiveService<String> {
    private ProcessMarketParseService processMarketParseSocket;
    private PublishService<Void, SymbolEntity> publishService;

    public ProcessMarketReceiveSocket(ProcessMarketParseService processMarketParseSocket,
            PublishService<Void, SymbolEntity> publishService) {
        this.processMarketParseSocket = processMarketParseSocket;
        this.publishService = publishService;
    }

    @Override
    public void process(String data) {
        SymbolEntity me = processMarketParseSocket.process(data);
        if (me == null) {
            System.err.println("Failed to parse data: " + data);
            return;
        }
        publishService.publish(me.getImtcode(), me);
    }

}
