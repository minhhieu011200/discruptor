package com.example.demo.infrastructure.worker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.infrastructure.mybatis.TranslogMapper;

@Configuration
public class TranslogConfig {

    private static final int WORKERS_PER_SHARD = 4;
    private static final int QUEUE_CAPACITY = 10_000;
    private static final int BATCH_SIZE = 5000;

    @Bean
    TranslogShardedQueue shardedQueue() {
        return new TranslogShardedQueue(QUEUE_CAPACITY);
    }

    @Bean
    void startWorkers(TranslogShardedQueue queue, TranslogMapper mapper) {
        ExecutorService executor = Executors.newCachedThreadPool();

        queue.getShardedQueues().forEach((symbol, shardQueue) -> {
            for (int i = 0; i < WORKERS_PER_SHARD; i++) {
                executor.submit(new TransLogWorker(shardQueue, mapper, BATCH_SIZE));
            }
        });
    }
}