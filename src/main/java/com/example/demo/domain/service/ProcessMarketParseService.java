package com.example.demo.domain.service;

import com.example.demo.domain.entity.SymbolEntity;

@FunctionalInterface
public interface ProcessMarketParseService<T> {
    SymbolEntity process(T data);
}
