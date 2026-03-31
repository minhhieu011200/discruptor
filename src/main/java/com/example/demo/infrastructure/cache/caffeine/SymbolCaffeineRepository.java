package com.example.demo.infrastructure.cache.caffeine;

import org.springframework.stereotype.Repository;

import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.repository.SymbolRepository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Repository
public class SymbolCaffeineRepository implements SymbolRepository {

    private final Cache<String, SymbolEntity> symbolCache = Caffeine.newBuilder()
            .maximumSize(10_000_000) // max 10 triệu entries
            .expireAfterAccess(24, TimeUnit.HOURS) // optional, tránh full heap
            .build();

    @Override
    public SymbolEntity get(String id) {
        return symbolCache.getIfPresent(id);
    }

    @Override
    public void set(String id, SymbolEntity entity) {
        symbolCache.put(id, entity);
    }

    @Override
    public void delete(String id) {
        symbolCache.invalidate(id);
    }

    @Override
    public void update(String id, SymbolEntity entity) {
        symbolCache.asMap().compute(id, (key, oldValue) -> {
            if (oldValue == null) {
                return entity;
            }
            if (entity.getExchangeid() != null) {
                oldValue.setExchangeid(entity.getExchangeid());
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
            if (entity.getRateQuoteID() != null) {
                oldValue.setRateQuoteID(entity.getRateQuoteID());
            }
            if (entity.getStatus() != null) {
                oldValue.setStatus(entity.getStatus());
            }
            return oldValue;
        });
    }

    @Override
    public Map<String, SymbolEntity> getAll() {
        return symbolCache.asMap();
    }
}