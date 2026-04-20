package com.example.demo.application.service;

import com.example.demo.application.dto.SymbolResponseDTO;
import com.example.demo.domain.entity.AccountEntity;
import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.repository.AccountRepository;
import com.example.demo.domain.repository.SymbolRepository;
import com.example.demo.domain.service.SymbolQueryService;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
public class SymbolQueryImplement implements SymbolQueryService {
    private final SymbolRepository symbolRepository;
    private final AccountRepository accountRepository;

    private static final int CONCURRENCY = 50;

    public SymbolQueryImplement(SymbolRepository symbolRepository,
            AccountRepository accountRepository) {
        this.symbolRepository = symbolRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public Mono<List<SymbolResponseDTO>> query(List<String> ids, String accountId) {
        return Flux.fromIterable(ids)
                // 👉 giới hạn concurrency + chạy trên boundedElastic (vì cache sync)
                .flatMap(id -> Mono.fromCallable(() -> symbolRepository.get(id))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(symbol -> {
                            if (symbol == null) {
                                return Mono.empty();
                            }
                            String key = buildAccountKey(accountId, symbol.getImtcode());

                            return Mono.fromCallable(() -> accountRepository.get(key))
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .defaultIfEmpty(new AccountEntity())
                                    .map(account -> SymbolResponseDTO.fromEntity(symbol, account));
                        }),
                        CONCURRENCY)

                .collectList();
    }

    private String buildAccountKey(String accountId, String imtcode) {
        return accountId + ":" + imtcode;
    }
}
