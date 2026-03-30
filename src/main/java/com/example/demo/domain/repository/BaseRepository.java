package com.example.demo.domain.repository;

public interface BaseRepository<T, P> {
    T get(P id);

    void set(P id, T entity);

    void delete(P id);

    void update(P id, T entity);
}
