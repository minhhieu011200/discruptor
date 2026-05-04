package com.example.demo.presentation.event;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Controller;

import com.example.demo.domain.service.PublishService;

@Controller
public class MarketDataKafka {
    private final PublishService<Void, byte[]> service;

    public MarketDataKafka(
            @Qualifier("DecodeDisruptor") PublishService<Void, byte[]> service) {
        this.service = service;
    }

    @KafkaListener(id = "${market.kafka.listener.id}", topics = "${market.kafka.listener.topics}", containerFactory = "batchFactory")
    public void listen(List<org.apache.kafka.clients.consumer.ConsumerRecord<String, byte[]>> records, Acknowledgment ack) {
        records.forEach(record -> {
            String traceId = null;
            if (record.headers() != null) {
                org.apache.kafka.common.header.Header header = record.headers().lastHeader("traceId");
                if (header != null) {
                    traceId = new String(header.value(), java.nio.charset.StandardCharsets.UTF_8);
                }
            }
            if (traceId == null) {
                traceId = java.util.UUID.randomUUID().toString();
            }
            org.slf4j.MDC.put("traceId", traceId);
            org.slf4j.MDC.put("startTime", String.valueOf(System.currentTimeMillis()));
            try {
                this.service.publish("", record.value());
            } finally {
                org.slf4j.MDC.remove("traceId");
                org.slf4j.MDC.remove("startTime");
            }
        });
        ack.acknowledge();
    }
}
