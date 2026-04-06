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

    public SymbolQueryImplement(SymbolRepository symbolRepository, AccountRepository accountRepository) {
        this.symbolRepository = symbolRepository;
        this.accountRepository = accountRepository;
    }

    public Mono<List<SymbolResponseDTO>> query(List<String> ids, String accountId) {
        return Flux.fromIterable(ids)
                .map(id -> {
                    SymbolEntity symbol = symbolRepository.get(id);
                    if (symbol == null)
                        return null;

                    AccountEntity account = accountRepository.get(accountId + symbol.getImtcode());
                    if (account == null)
                        account = new AccountEntity();

                    return SymbolResponseDTO.fromEntity(symbol, account);
                })
                .filter(Objects::nonNull)
                .collectList(); // -> Mono<List<SymbolResponseDTO>>
    }
}
