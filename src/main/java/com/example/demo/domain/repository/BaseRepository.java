package com.example.demo.domain.repository;

import java.util.Map;

import com.example.demo.domain.entity.SymbolEntity;

public interface BaseRepository<T, P> {
    T get(P id);

    void set(P id, T entity);

    void delete(P id);

    void update(P id, T entity);

    Map<P, T> getAll();
}
