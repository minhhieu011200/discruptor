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
    private double buyPriceTesury;
    private double sellPriceTesury;

    public void setImtCode() {
        this.imtcode = buyCurrency + sellCurrency;
    }

    public void setBuyPriceTesury() {
        this.buyPriceTesury = this.ask + 1;
    }

    public void setSellPriceTesury() {
        this.sellPriceTesury = this.bid - 1;
    }
}