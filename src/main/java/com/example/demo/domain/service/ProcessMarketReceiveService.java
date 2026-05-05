package com.example.demo.domain.service;

@FunctionalInterface
public interface ProcessMarketReceiveService {
    void process(byte[] data, String traceid, Long startTime);
}
