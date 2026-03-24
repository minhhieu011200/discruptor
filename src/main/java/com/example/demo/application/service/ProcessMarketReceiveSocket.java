package com.example.demo.application.service;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import com.example.demo.domain.entity.MarketEntity;
import com.example.demo.domain.service.ProcessMarketParseService;
import com.example.demo.domain.service.ProcessMarketReceiveService;
import com.example.demo.domain.service.PublishService;

@Service
public class ProcessMarketReceiveSocket implements ProcessMarketReceiveService<String> {
    private ProcessMarketParseService processMarketParseSocket;
    private PublishService<Void, MarketEntity> publishService;

    public ProcessMarketReceiveSocket(ProcessMarketParseService processMarketParseSocket,
            PublishService<Void, MarketEntity> publishService) {
        this.processMarketParseSocket = processMarketParseSocket;
        this.publishService = publishService;
    }

    @Override
    public void process(String data) {
        for (int i = 0; i < 10000000; i++) {
            MarketEntity me = new MarketEntity();
            me.setSymbol(8800 + ThreadLocalRandom.current().nextInt(100));
            me.setBid(3);
            me.setAsk(4);
            me.setLast(i);
            publishService.publish(me);
        }
    }

}
