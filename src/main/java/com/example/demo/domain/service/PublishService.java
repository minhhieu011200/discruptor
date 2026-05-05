package com.example.demo.domain.service;

public interface PublishService<T, D> {
    T publish(String channel, D data);

    T publishTrace(String channel, D data, String traceId, long startTime);
}
