package com.example.demo.domain.entity;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.Data;
import net.openhft.chronicle.bytes.BytesMarshallable;

@Data
public class SymbolEntity implements BytesMarshallable {
    private double bid;
    private double ask;
    private String buyCurrency;
    private String sellCurrency;
    private String tenor;
    private String status;
    private String imtcode;
    private double spread;
    private long validFrom;
    private long validTill;
    private String rateQuoteID;
    private long version;

    public void setImtCode() {
        this.imtcode = buyCurrency + sellCurrency;
    }

    public static SymbolEntity cloneLight(SymbolEntity src) {
        SymbolEntity s = new SymbolEntity();
        s.bid = src.bid;
        s.ask = src.ask;
        s.spread = src.spread;
        s.validFrom = src.validFrom;
        s.validTill = src.validTill;
        s.version = src.version;
        s.buyCurrency = src.buyCurrency;
        s.sellCurrency = src.sellCurrency;
        s.tenor = src.tenor;
        s.status = src.status;
        s.imtcode = src.imtcode;
        s.rateQuoteID = src.rateQuoteID;

        return s;
    }
}