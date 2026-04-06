package com.example.demo.domain.service;

import java.util.List;

import com.example.demo.application.dto.SymbolResponseDTO;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface SymbolQueryService {
    Mono<List<SymbolResponseDTO>> query(List<String> ids, String accountid);
}
