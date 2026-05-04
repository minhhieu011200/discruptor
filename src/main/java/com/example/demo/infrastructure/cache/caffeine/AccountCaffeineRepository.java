package com.example.demo.infrastructure.cache.caffeine;

import java.util.Map;

import org.springframework.stereotype.Repository;

import com.example.demo.domain.entity.AccountEntity;
import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.repository.AccountRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

// @Repository
public class AccountCaffeineRepository implements AccountRepository {
    private final Cache<String, AccountEntity> accountImtCodeCache = Caffeine.newBuilder()
            .maximumSize(10_000_000) // max 10 triệu entries
            .build();

    @Override
    public AccountEntity get(String id) {
        return accountImtCodeCache.getIfPresent(id);
    }

    @Override
    public void set(String id, AccountEntity entity) {
        accountImtCodeCache.put(id, entity);
    }

    @Override
    public void delete(String id) {
        accountImtCodeCache.invalidate(id);
    }

    @Override
    public void update(String id, AccountEntity entity) {
        accountImtCodeCache.asMap().compute(id, (key, oldValue) -> {
            if (oldValue == null) {
                return entity;
            }
            return oldValue;
        });
    }

    @Override
    public Map<String, AccountEntity> getAll() {
        return accountImtCodeCache.asMap();
    }
}
