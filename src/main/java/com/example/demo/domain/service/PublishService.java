package com.example.demo.domain.service;

@FunctionalInterface
public interface PublishService<T, D> {
    T publish(D data);
}
