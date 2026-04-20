package com.example.demo.application.dto;

import java.util.List;

public record SubscribeExchangeRequestDTO(List<SubscribeExchangeDTO> currencyPairList) {
}