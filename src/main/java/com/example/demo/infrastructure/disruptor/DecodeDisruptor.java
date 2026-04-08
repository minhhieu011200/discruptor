package com.example.demo.infrastructure.disruptor;

import org.springframework.stereotype.Service;

import com.example.demo.domain.service.ProcessMarketReceiveService;
import com.example.demo.domain.service.PublishService;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import jakarta.annotation.PreDestroy;

@Service("DecodeDisruptor")
public class DecodeDisruptor implements PublishService<Void, byte[]> {
    private final Disruptor<DecodeEvent> disruptorDecode;
    private final RingBuffer<DecodeEvent> ringBufferDecode;

    private static final int RING_BUFFER_SIZE = 1024; // power
    private final ProcessMarketReceiveService<byte[]> service;

    public DecodeDisruptor(ProcessMarketReceiveService<byte[]> processMarketReceiveService) throws Exception {
        this.service = processMarketReceiveService;

        disruptorDecode = new Disruptor<>(
                new DecodeEvent.Factory(),
                RING_BUFFER_SIZE,
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

    @PreDestroy
    public void shutdown() {
        System.out.println("[DecodeDisruptor] Shutting down...");
        disruptorDecode.shutdown();
    }
}
