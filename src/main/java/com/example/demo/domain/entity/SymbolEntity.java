package com.example.demo.domain.entity;

import lombok.Data;

@Data
public class SymbolEntity extends BaseEntity {
    private String exchangeid;
    private String buyCurrency;
    private String sellCurrency;
    private String imtcode;
    private long bid;
    private long ask;
    private String tenor;
    private double spread;
    private String rateQuoteID;
    private String status;
}
