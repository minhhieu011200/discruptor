package com.example.demo.infrastructure.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;

import com.example.demo.domain.service.PublishService;

public class RedisPubSub implements PublishService<Void, String> {
    private final StringRedisTemplate redisTemplate;

    public RedisPubSub(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Void publish(String channel, String message) {
        redisTemplate.convertAndSend(channel, message);
        return null;
    }

}
