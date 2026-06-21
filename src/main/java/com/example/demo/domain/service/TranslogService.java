package com.example.demo.domain.service;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.entity.TranslogEntity;
import com.example.demo.infrastructure.mybatis.TranslogMapper;

@Service
@ConditionalOnProperty(name = "app.connection.postgres.enabled", havingValue = "true", matchIfMissing = false)
public class TranslogService {
    private final TranslogMapper mapper;

    public TranslogService(TranslogMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public void insertBatch(List<TranslogEntity> batch) {
        if (!batch.isEmpty()) {
            mapper.insertBatch(batch);
        }
    }
}
