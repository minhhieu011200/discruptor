package com.example.demo.infrastructure.cache.chronicle;

import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.repository.SymbolRepository;

import net.openhft.chronicle.map.ChronicleMap;

// @Repository
public class SymbolChronicleRepository implements SymbolRepository {
    ChronicleMap<String, SymbolEntity> symbolChronicleMap = ChronicleMap
            .of(String.class, SymbolEntity.class)
            .name("symbol-price-map")
            .entries(10_000000)
            .averageKeySize(64) // khoảng 20 ký tự trung bình cho key
            .averageValueSize(200)
            .create();

    @Override
    public SymbolEntity get(String id) {
        return symbolChronicleMap.get(id);
    }

    @Override
    public void set(String id, SymbolEntity entity) {
        symbolChronicleMap.put(id, entity);
    }

    @Override
    public void delete(String id) {
        symbolChronicleMap.remove(id);
    }

    @Override
    public void update(String id, SymbolEntity entity) {
        symbolChronicleMap.compute(id, (key, oldValue) -> {
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
    public ChronicleMap<String, SymbolEntity> getAll() {
        return symbolChronicleMap;
    }
}
