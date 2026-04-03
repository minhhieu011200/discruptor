package com.example.demo.infrastructure.cache.caffeine;

import java.util.Map;

import org.springframework.stereotype.Repository;

import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.repository.AccountRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Repository
public class AccountCaffeineRepository implements AccountRepository {
    private final Cache<String, SymbolEntity> accountImtCodeCache = Caffeine.newBuilder()
            .maximumSize(10_000_000) // max 10 triệu entries
            .build();

    @Override
    public SymbolEntity get(String id) {
        return accountImtCodeCache.getIfPresent(id);
    }

    @Override
    public void set(String id, SymbolEntity entity) {
        accountImtCodeCache.put(id, entity);
    }

    @Override
    public void delete(String id) {
        accountImtCodeCache.invalidate(id);
    }

    @Override
    public void update(String id, SymbolEntity entity) {
        accountImtCodeCache.asMap().compute(id, (key, oldValue) -> {
            if (oldValue == null) {
                return entity;
            }
            if (entity.getBuyCurrency() != null) {
                oldValue.setBuyCurrency(entity.getBuyCurrency());
            }
            if (entity.getSellCurrency() != null) {
                oldValue.setSellCurrency(entity.getSellCurrency());
            }
            if (entity.getImtcode() != null) {
                oldValue.setImtcode(entity.getImtcode());
            }
            if (entity.getBid() != 0) {
                oldValue.setBid(entity.getBid());
            }
            if (entity.getAsk() != 0) {
                oldValue.setAsk(entity.getAsk());
            }
            if (entity.getTenor() != null) {
                oldValue.setTenor(entity.getTenor());
            }
            if (entity.getSpread() != 0) {
                oldValue.setSpread(entity.getSpread());
            }
            if (entity.getStatus() != null) {
                oldValue.setStatus(entity.getStatus());
            }
            return oldValue;
        });
    }

    @Override
    public Map<String, SymbolEntity> getAll() {
        return accountImtCodeCache.asMap();
    }
}
