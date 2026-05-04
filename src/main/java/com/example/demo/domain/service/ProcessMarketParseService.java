package com.example.demo.domain.service;

import com.example.demo.application.dto.SymbolRequestDTO;

@FunctionalInterface
public interface ProcessMarketParseService {
    SymbolRequestDTO process(byte[] data);
}
