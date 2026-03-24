package com.example.demo.domain.service;

import com.example.demo.domain.entity.MarketEntity;

@FunctionalInterface
public interface ProcessMarketParseService<T> {
    MarketEntity process(T data);
}
