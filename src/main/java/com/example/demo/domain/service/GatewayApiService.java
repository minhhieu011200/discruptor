package com.example.demo.domain.service;

import com.example.demo.application.dto.SubscribeExchangeRequestDTO;

public interface GatewayApiService {
    void subscribe(SubscribeExchangeRequestDTO data);

    void unsubscribe(SubscribeExchangeRequestDTO data);
}
