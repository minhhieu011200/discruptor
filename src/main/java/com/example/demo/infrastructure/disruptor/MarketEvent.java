package com.example.demo.infrastructure.disruptor;

import com.example.demo.domain.entity.MarketEntity;
import com.lmax.disruptor.EventFactory;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class MarketEvent {
    private MarketEntity entity;

    public MarketEntity getEntity() {
        return entity;
    }

    public void setEntity(MarketEntity entity) {
        this.entity = entity;
    }

    public static class Factory implements EventFactory<MarketEvent> {
        @Override
        public MarketEvent newInstance() {
            return new MarketEvent();
        }
    }
}
