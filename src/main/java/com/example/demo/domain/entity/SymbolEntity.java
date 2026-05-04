package com.example.demo.domain.entity;

import lombok.Data;
import net.openhft.chronicle.bytes.BytesMarshallable;

@Data
public class SymbolEntity extends BaseEntity implements BytesMarshallable {
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

    public void setImtCode() {
        this.imtcode = buyCurrency + sellCurrency;
    }

}