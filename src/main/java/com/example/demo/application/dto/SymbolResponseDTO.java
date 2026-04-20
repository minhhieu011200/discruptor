package com.example.demo.application.dto;

import com.example.demo.domain.entity.AccountEntity;
import com.example.demo.domain.entity.SymbolEntity;

public record SymbolResponseDTO(
        double bid,
        double ask,
        String tenor,
        String status,
        String imtcode,
        double spread,
        double marginBuyAccount,
        double marginSellAccount,
        double buyPriceTesury) {
    public static SymbolResponseDTO fromEntity(SymbolEntity e, AccountEntity a) {
        double marginBuy = a != null ? a.getMarginBuy() : 0;
        double marginSell = a != null ? a.getMarginSell() : 0;

        return new SymbolResponseDTO(
                e.getBid(),
                e.getAsk(),
                e.getTenor(),
                e.getStatus(),
                e.getImtcode(),
                e.getSpread(),
                marginBuy,
                marginSell,
                e.getAsk() + e.getSpread() + marginBuy);
    }
}