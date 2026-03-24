package com.example.demo.domain.service;

@FunctionalInterface
public interface ProcessMarketReceiveService<T> {
    void process(T data);
}
