package com.example.demo.infrastructure.disruptor;

import com.example.demo.domain.entity.SymbolEntity;
import com.lmax.disruptor.EventFactory;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class MarketEvent {
    private SymbolEntity entity;

    public SymbolEntity getEntity() {
        return entity;
    }

    public void setEntity(SymbolEntity entity) {
        this.entity = entity;
    }

    public static class Factory implements EventFactory<MarketEvent> {
        @Override
        public MarketEvent newInstance() {
            return new MarketEvent();
        }
    }
}
