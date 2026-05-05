package com.example.demo.presentation.event;

import java.util.List;

import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Controller;

import com.example.demo.domain.service.PublishDecodeDisruptor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class MarketDataKafka {
    private final PublishDecodeDisruptor service;

    public MarketDataKafka(PublishDecodeDisruptor service) {
        this.service = service;
    }

    @KafkaListener(id = "${market.kafka.listener.id}", topics = "${market.kafka.listener.topics}", containerFactory = "batchFactory")
    public void listen(List<org.apache.kafka.clients.consumer.ConsumerRecord<String, byte[]>> records,
            Acknowledgment ack) {
        records.forEach(record -> {

            long startTime = System.nanoTime();
            String traceId = null;
            if (record.headers() != null) {
                org.apache.kafka.common.header.Header header = record.headers().lastHeader("rateQuoteID");
                if (header != null) {
                    traceId = new String(header.value(),
                            java.nio.charset.StandardCharsets.UTF_8);
                }
            }
            if (traceId == null) {
                traceId = java.util.UUID.randomUUID().toString();
            }
            this.service.publishTrace("", record.value(), traceId, startTime);
        });
        ack.acknowledge();
    }
}
