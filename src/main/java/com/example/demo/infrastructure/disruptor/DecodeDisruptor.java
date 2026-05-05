package com.example.demo.infrastructure.disruptor;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.example.demo.domain.service.ProcessMarketReceiveService;
import com.example.demo.domain.service.PublishDecodeDisruptor;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DecodeDisruptor implements PublishDecodeDisruptor {
    private final Disruptor<DecodeEvent> disruptorDecode;
    private final RingBuffer<DecodeEvent> ringBufferDecode;

    private final int ringBufferSize;
    private final ProcessMarketReceiveService service;

    public DecodeDisruptor(
            ProcessMarketReceiveService processMarketReceiveService,
            @Value("${decode.ring.buffer.size:1024}") int ringBufferSize) throws Exception {
        this.service = processMarketReceiveService;
        this.ringBufferSize = ringBufferSize;

        disruptorDecode = new Disruptor<>(
                new DecodeEvent.Factory(),
                this.ringBufferSize,
                Thread.ofPlatform().name("decode-worker").factory(),
                ProducerType.SINGLE,
                new BlockingWaitStrategy());

        disruptorDecode.handleEventsWith(new DecodeEventHandler(service));
        disruptorDecode.start();
        // Lấy ringBuffer sau khi start
        ringBufferDecode = disruptorDecode.getRingBuffer();
    }

    @Override
    public Void publish(String channel, byte[] data) {
        long sequence = ringBufferDecode.next();
        try {
            DecodeEvent event = ringBufferDecode.get(sequence);
            event.data = data;
        } finally {
            ringBufferDecode.publish(sequence);
        }
        return null;
    }

    @Override
    public Void publishTrace(String channel, byte[] data, String traceId, long startTime) {
        long sequence = ringBufferDecode.next();
        try {
            DecodeEvent event = ringBufferDecode.get(sequence);
            event.data = data;
            event.traceId = traceId;
            event.startTime = startTime;
        } finally {
            ringBufferDecode.publish(sequence);
        }
        return null;
    }

    @PreDestroy
    public void shutdown() {
        log.info("[DecodeDisruptor] Shutting down...");
        disruptorDecode.shutdown();
    }
}
