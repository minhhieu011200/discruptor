package com.example.demo.domain.entity;

import lombok.Builder;
import lombok.Data;
import lombok.With;

@Data
@Builder
@With
public class SymbolEntity extends BaseEntity {
    private String exchangeid;
    private String buyCurrency;
    private String sellCurrency;
    private String imtcode;
    private double bid;
    private double ask;
    private String tenor;
    private double spread;
    private String rateQuoteID;
    private String status;
}