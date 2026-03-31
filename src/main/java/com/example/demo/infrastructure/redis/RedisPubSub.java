package com.example.demo.infrastructure.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.example.demo.domain.service.PublishService;

@Service("RedisPubSub")
public class RedisPubSub<T> implements PublishService<Void, T> {
    private final StringRedisTemplate redisTemplate;

    public RedisPubSub(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Void publish(String channel, T message) {
        redisTemplate.convertAndSend(channel, message);
        return null;
    }

}
