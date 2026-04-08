package com.example.demo.presentation.controller;

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
    public void listen(List<byte[]> batch, Acknowledgment ack) {
        batch.forEach(data -> this.service.publish("", data));
        ack.acknowledge();
    }
}
