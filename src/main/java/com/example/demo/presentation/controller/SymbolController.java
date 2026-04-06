package com.example.demo.presentation.controller;

import com.example.demo.application.dto.SymbolQueryRequestDTO;
import com.example.demo.application.dto.SymbolResponseDTO;
import com.example.demo.application.service.SymbolQueryImplement;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/symbols")
public class SymbolController {
    private final SymbolQueryImplement symbolQueryService;

    public SymbolController(SymbolQueryImplement symbolQueryService) {
        this.symbolQueryService = symbolQueryService;
    }

    @PostMapping("/batch")
    public Mono<List<SymbolResponseDTO>> query(@RequestBody SymbolQueryRequestDTO req) {
        return symbolQueryService.query(req.getIds(), req.getAccountId());
    }
}
