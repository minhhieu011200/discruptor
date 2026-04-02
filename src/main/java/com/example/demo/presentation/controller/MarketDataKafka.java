package com.example.demo.presentation.controller;

import java.nio.ByteBuffer;
import java.util.List;

import org.agrona.concurrent.UnsafeBuffer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Controller;

import com.etrade.gateway.sbe.BooleanType;
import com.etrade.gateway.sbe.MessageHeaderDecoder;
import com.etrade.gateway.sbe.QuoteDecoder;
import com.example.demo.domain.service.PublishService;
import com.example.demo.infrastructure.metrics.MessageThroughputMetrics;

@Controller
public class MarketDataKafka {
    private final PublishService<Void, byte[]> service;
    private final MessageThroughputMetrics metrics;

    public MarketDataKafka(
            @Qualifier("DecodeDisruptor") PublishService<Void, byte[]> service,
            MessageThroughputMetrics metrics) {
        this.service = service;
        this.metrics = metrics;
    }

    @KafkaListener(id = "${market.kafka.listener.id}", topics = "${market.kafka.listener.topics}", containerFactory = "batchFactory")
    public void listen(List<byte[]> batch, Acknowledgment ack) {
        batch.forEach(data -> this.service.publish("", data));
        try {
            this.metrics.recordMessages(batch.size(), "kafka_listener");
        } catch (Exception e) {
            // do not break processing if metrics record fails
        }
        ack.acknowledge();
    }
}
