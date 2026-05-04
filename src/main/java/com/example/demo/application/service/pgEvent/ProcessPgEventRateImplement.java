package com.example.demo.application.service.pgEvent;

import com.example.demo.application.annotation.Measured;
import com.example.demo.application.annotation.PgEventType;
import com.example.demo.application.utils.JsonUtils;
import com.example.demo.domain.entity.SymbolEntity;

import com.example.demo.domain.repository.SymbolRepository;
import com.example.demo.domain.service.ProcessPgEventService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@PgEventType("RATE_ENGINE")
@Component
public class ProcessPgEventRateImplement implements ProcessPgEventService {

    private final ObjectMapper objectMapper;
    private final SymbolRepository symbolRepository;

    public ProcessPgEventRateImplement(SymbolRepository symbolRepository, ObjectMapper objectMapper) {
        this.symbolRepository = symbolRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Measured(value = "pg.event.rate", description = "Time to process rate event")
    public void process(JsonNode message) {
        JsonNode refidNode = JsonUtils.parseRefId(objectMapper, message.path("refid").asText());
        if (refidNode == null)
            return;

        String imtcode = JsonUtils.extractText(refidNode, "IMTCODE");
        Double bidObj = JsonUtils.extractDouble(refidNode, "BUYRATE");
        Double askObj = JsonUtils.extractDouble(refidNode, "SELLRATE");
        double bid = bidObj != null ? bidObj : 0.0;
        double ask = askObj != null ? askObj : 0.0;

        SymbolEntity symbol = symbolRepository.get(imtcode);
        if (symbol == null) {
            symbol = new SymbolEntity();
            symbol.setImtcode(imtcode);
            symbol.setBid(bid);
            symbol.setAsk(ask);
            symbol.setVersion();
            symbolRepository.set(imtcode, symbol);
            return;
        }

        symbol.setBid(bid);
        symbol.setAsk(ask);
        symbol.setVersion();
    }
}
