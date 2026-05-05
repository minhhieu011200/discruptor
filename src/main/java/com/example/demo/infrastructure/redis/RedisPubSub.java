package com.example.demo.infrastructure.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.example.demo.domain.service.PublishRedis;

@Service("RedisPubSub")
public class RedisPubSub<T> implements PublishRedis<T> {
    private final StringRedisTemplate redisTemplate;

    public RedisPubSub(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Void publish(String channel, T message) {
        redisTemplate.convertAndSend(channel, message);
        return null;
    }

    @Override
    public Void publishTrace(String channel, T data, String traceId, long startTime) {
        redisTemplate.convertAndSend(channel, data);
        return null;
    }
}
