package com.example.demo.infrastructure.cache.chronicle;

import net.openhft.chronicle.hash.ChronicleHashBuilder;
import net.openhft.chronicle.map.ChronicleMap;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.entity.AccountEntity;
import com.example.demo.domain.repository.AccountRepository;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Repository
public class AccountChronicleRepository implements AccountRepository {

    private ChronicleMap<String, AccountEntity> accountCache;

    @PostConstruct
    public void init() throws IOException {
        File file = new File("account-cache.dat");

        this.accountCache = ChronicleMap
                .of(String.class, AccountEntity.class)
                .name("account-cache")
                .averageKeySize(50)
                .averageValueSize(512)
                .entries(1_000_000)
                .recoverPersistedTo(file, false);
    }

    @Override
    public AccountEntity get(String id) {
        return accountCache.get(id);
    }

    @Override
    public void set(String id, AccountEntity entity) {
        accountCache.put(id, entity);
    }

    @Override
    public void delete(String id) {
        accountCache.remove(id);
    }

    @Override
    public void update(String id, AccountEntity entity) {
        accountCache.compute(id, (key, oldValue) -> {
            if (oldValue == null)
                return entity;
            return oldValue; // same logic như bạn viết
        });
    }

    @Override
    public Map<String, AccountEntity> getAll() {
        return accountCache;
    }
}