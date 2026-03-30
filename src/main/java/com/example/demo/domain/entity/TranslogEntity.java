package com.example.demo.domain.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TranslogEntity extends BaseEntity {
    private String imtcode;
    private String buyCurrency;
    private String sellCurrency;
    private double bid;
    private double ask;
    private String tenor;
    private double spread;
    private double priceTesury;
}
