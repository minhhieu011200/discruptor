package com.example.demo.application.dto;

import lombok.Data;

@Data
public class SymbolRequestDTO {
    private double bid;
    private double ask;
    private boolean valid;
    private long validFrom;
    private long validTill;
    private String rateType;
    private String rateQuoteID;
    private String rateCategoryID;
    private String buyCurrency;
    private String sellCurrency;
    private String tenor;
    private String status;

    private String imtcode;

    public void hydrate(
            double bid,
            double ask,
            boolean valid,
            long validFrom,
            long validTill,
            String rateType,
            String rateQuoteID,
            String rateCategoryID,
            String baseCurrency,
            String quoteCurrency,
            String tenor,
            String status) {
        this.bid = bid;
        this.ask = ask;
        this.valid = valid;
        this.validFrom = validFrom;
        this.validTill = validTill;

        this.rateType = rateType;
        this.rateQuoteID = rateQuoteID;
        this.rateCategoryID = rateCategoryID;

        this.buyCurrency = baseCurrency;
        this.sellCurrency = quoteCurrency;
        this.tenor = tenor;
        this.status = status;
        this.imtcode = baseCurrency + quoteCurrency;
    }
}
