package com.example.demo.domain.entity;

import lombok.Data;

@Data
public class MarketEntity {
    private Integer symbol;
    private long bid;
    private long ask;
    private long last;
}
