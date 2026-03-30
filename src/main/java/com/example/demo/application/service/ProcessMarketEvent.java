package com.example.demo.application.service;

import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.entity.TranslogEntity;
import com.example.demo.domain.repository.SymbolRepository;
import com.example.demo.domain.repository.TranslogShardedQueueRepository;
import com.example.demo.domain.service.ProcessMarketEventService;
import com.example.demo.domain.service.PublishService;
import org.springframework.stereotype.Service;

@Service
public class ProcessMarketEvent implements ProcessMarketEventService {

    private final PublishService<String, SymbolEntity> publishService;

    private final SymbolRepository symbolRepository;
    private final TranslogShardedQueueRepository translogShardedQueueRepository;

    public ProcessMarketEvent(PublishService<String, SymbolEntity> publishService, SymbolRepository symbolRepository,
            TranslogShardedQueueRepository translogShardedQueueRepository) {
        this.publishService = publishService;
        this.symbolRepository = symbolRepository;
        this.translogShardedQueueRepository = translogShardedQueueRepository;
    }

    @Override
    public void process(SymbolEntity data) {
        String imt = data.getImtcode();

        if ("USDVND".equals(imt)) {

        } else {
            data.setUpdatedTime(System.currentTimeMillis());
            symbolRepository.set(imt, data);
            TranslogEntity translogEntity = TranslogEntity.builder()
                    .imtcode(imt)
                    .bid(data.getBid())
                    .ask(data.getAsk())
                    .spread(data.getSpread())
                    .build();
            translogEntity.setUpdatedTime(System.currentTimeMillis());
            translogShardedQueueRepository.offer(translogEntity);
        }
    }
}