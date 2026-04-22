package com.example.demo.domain.entity;

import lombok.Data;

@Data
public class SymbolEntity extends BaseEntity {
    private double bid;
    private double ask;
    private String buyCurrency;
    private String sellCurrency;
    private String tenor;
    private String status;
    private String imtcode;
    private double spread;

    public void setImtCode() {
        this.imtcode = buyCurrency + sellCurrency;
    }

}