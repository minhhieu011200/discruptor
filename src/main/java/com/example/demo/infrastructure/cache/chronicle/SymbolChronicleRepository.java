package com.example.demo.infrastructure.cache.chronicle;

import net.openhft.chronicle.map.ChronicleMap;
import org.springframework.stereotype.Repository;
import jakarta.annotation.PostConstruct;

import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.repository.SymbolRepository;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Repository
public class SymbolChronicleRepository implements SymbolRepository {

    private ChronicleMap<String, SymbolEntity> symbolCache;

    @PostConstruct
    public void init() throws IOException {
        File file = new File("symbol-cache.dat");

        this.symbolCache = ChronicleMap
                .of(String.class, SymbolEntity.class)
                .name("symbol-cache")
                .averageKeySize(32)
                .averageValueSize(256)
                .entries(1_000_000)
                // Dùng hàm tự động phá khóa và phục hồi nếu file bị lỗi kẹt lock
                .recoverPersistedTo(file, false);

    }

    @Override
    public SymbolEntity get(String id) {
        return symbolCache.get(id);
    }

    @Override
    public void set(String id, SymbolEntity entity) {
        symbolCache.put(id, entity);
    }

    @Override
    public void delete(String id) {
        symbolCache.remove(id);
    }

    @Override
    public void update(String id, SymbolEntity entity) {
        symbolCache.compute(id, (key, oldValue) -> {
            if (oldValue == null)
                return entity;

            if (entity.getBuyCurrency() != null)
                oldValue.setBuyCurrency(entity.getBuyCurrency());
            if (entity.getSellCurrency() != null)
                oldValue.setSellCurrency(entity.getSellCurrency());
            if (entity.getImtcode() != null)
                oldValue.setImtcode(entity.getImtcode());
            if (entity.getBid() != 0)
                oldValue.setBid(entity.getBid());
            if (entity.getAsk() != 0)
                oldValue.setAsk(entity.getAsk());
            if (entity.getTenor() != null)
                oldValue.setTenor(entity.getTenor());
            if (entity.getSpread() != 0)
                oldValue.setSpread(entity.getSpread());
            if (entity.getStatus() != null)
                oldValue.setStatus(entity.getStatus());

            return oldValue;
        });
    }

    @Override
    public Map<String, SymbolEntity> getAll() {
        return symbolCache; // ChronicleMap implements Map
    }
}