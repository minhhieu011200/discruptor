package com.example.demo.domain.entity;

import lombok.Data;
import net.openhft.chronicle.bytes.BytesMarshallable;

@Data
public class AccountEntity extends BaseEntity implements BytesMarshallable {
    private String cifid;
    private String imtcode;
    private double marginBuy;
    private double marginSell;
}
