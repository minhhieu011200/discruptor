package com.example.demo.application.service;

import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.entity.TranslogEntity;
import com.example.demo.domain.repository.SymbolRepository;
import com.example.demo.domain.repository.TranslogShardedQueueRepository;
import com.example.demo.domain.service.ProcessMarketEventService;
import com.example.demo.domain.service.PublishService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ProcessMarketEvent implements ProcessMarketEventService {
    private final PublishService<Void, SymbolEntity> redisPublishService;

    private final SymbolRepository symbolRepository;
    private final TranslogShardedQueueRepository translogShardedQueueRepository;

    public ProcessMarketEvent(
            @Qualifier("RedisPubSub") PublishService<Void, SymbolEntity> redisPublishService,
            SymbolRepository symbolRepository,
            TranslogShardedQueueRepository translogShardedQueueRepository) {
        this.redisPublishService = redisPublishService;
        this.symbolRepository = symbolRepository;
        this.translogShardedQueueRepository = translogShardedQueueRepository;
    }

    @Override
    public void process(SymbolEntity data) {
        System.out.println("Processing market event for symbol: " + data.toString());
        String imt = data.getImtcode();

        if ("USDVND".equals(imt)) {

        } else {
            // symbolRepository.set(imt, data);
            TranslogEntity translogEntity = TranslogEntity.builder()
                    .imtcode(imt)
                    .bid(data.getBid())
                    .ask(data.getAsk())
                    .spread(data.getSpread())
                    .build();
            translogEntity.setVersion();
            translogShardedQueueRepository.offer(translogEntity);
            // this.redisPublishService.publish(imt, data);
        }
    }
}