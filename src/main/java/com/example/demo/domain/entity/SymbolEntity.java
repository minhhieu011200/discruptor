package com.example.demo.domain.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
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
