package com.example.demo.domain.entity;

import lombok.Builder;
import lombok.Data;

@Data
public class TranslogEntity extends BaseEntity {
    private String imtcode;
    private String buyCurrency;
    private String sellCurrency;
    private double bid;
    private double ask;
    private String tenor;
    private double spread;
    private double buyPriceTesury;
    private double sellPriceTesury;;

    public void hydrate(
            String imtcode,
            String buyCurrency,
            String sellCurrency,
            String tenor,
            double bid,
            double ask,
            double spread,
            double buyPriceTesury,
            double sellPriceTesury) {
        this.imtcode = imtcode;
        this.buyCurrency = buyCurrency;
        this.sellCurrency = sellCurrency;
        this.tenor = tenor;
        this.bid = bid;
        this.ask = ask;
        this.spread = spread;
        this.buyPriceTesury = buyPriceTesury;
        this.sellPriceTesury = sellPriceTesury;
        setVersion();
    }
}
