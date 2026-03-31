package com.example.demo.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.entity.TranslogEntity;
import com.example.demo.infrastructure.mybatis.TranslogMapper;

@Service
public class TranslogService {
    private final TranslogMapper mapper;

    public TranslogService(TranslogMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public void insertBatch(List<TranslogEntity> batch) {
        if (!batch.isEmpty()) {
            mapper.insertBatch(batch);
            System.out.println("Inserted batch size: " + batch.size());
        }
    }
}
