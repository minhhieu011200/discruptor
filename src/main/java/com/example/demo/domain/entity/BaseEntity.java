package com.example.demo.domain.entity;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Data;

@Data
public class BaseEntity {

    private long createdTime;
    private long updatedTime;

    private int version;
    private int lastFlushedVersion;

    public void setVersion() {
        version++;
        updatedTime = System.currentTimeMillis();
    }

    public boolean isDirty() {
        return version != lastFlushedVersion;
    }

    public void markClean() {
        lastFlushedVersion = version;
    }
}