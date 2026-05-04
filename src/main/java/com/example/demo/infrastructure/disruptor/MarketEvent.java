package com.example.demo.infrastructure.disruptor;

import com.example.demo.application.dto.SymbolRequestDTO;

import com.lmax.disruptor.EventFactory;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class MarketEvent {
    private SymbolRequestDTO entity;

    public SymbolRequestDTO getEntity() {
        return entity;
    }

    public void setEntity(SymbolRequestDTO entity) {
        this.entity = entity;
    }

    public static class Factory implements EventFactory<MarketEvent> {
        @Override
        public MarketEvent newInstance() {
            return new MarketEvent();
        }
    }
}
