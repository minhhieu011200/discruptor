package com.example.demo.domain.entity;

import lombok.Data;

@Data
public class AccountEntity extends BaseEntity {
    private String cifid;
    private String imtcode;
    private double marginBuy;
    private double marginSell;
}
