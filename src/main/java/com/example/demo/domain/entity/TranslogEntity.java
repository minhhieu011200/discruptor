package com.example.demo.domain.entity;

import lombok.Builder;
import lombok.Data;

@Data
public class TranslogEntity {
    private String imtcode;
    private String buyCurrency;
    private String sellCurrency;
    private double bid;
    private double ask;
    private String tenor;
    private double spread;
    private long validFrom;
    private long validTill;
    private String rateQuoteID;
    private long version;

    public void hydrate(
            String imtcode,
            String buyCurrency,
            String sellCurrency,
            String tenor,
            double bid,
            double ask,
            double spread,
            long validFrom,
            long validTill,
            String rateQuoteID,
            long version) {
        this.imtcode = imtcode;
        this.buyCurrency = buyCurrency;
        this.sellCurrency = sellCurrency;
        this.tenor = tenor;
        this.bid = bid;
        this.ask = ask;
        this.spread = spread;
        this.validFrom = validFrom;
        this.validTill = validTill;
        this.rateQuoteID = rateQuoteID;
        this.version = version;
    }
}
