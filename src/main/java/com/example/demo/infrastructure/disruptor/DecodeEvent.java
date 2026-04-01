package com.example.demo.infrastructure.disruptor;

import com.lmax.disruptor.EventFactory;

import lombok.Data;

@Data
public class DecodeEvent {
    public byte[] data;

    public static class Factory implements EventFactory<DecodeEvent> {
        @Override
        public DecodeEvent newInstance() {
            return new DecodeEvent();
        }
    }
}