package com.example.demo.domain.entity;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Data;

@Data
public class BaseEntity {

    private long createdTime;
    private long updatedTime;

    private volatile int version;

    public void setVersion(int version) {
        updatedTime = System.nanoTime();
        this.version = version;
    }

}