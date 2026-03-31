package com.example.demo.domain.entity;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@Builder
public class AccountEntity extends BaseEntity {
    private String cifid;
    private String imtcode;
    private double marginBuy;
    private double marginSell;
}
