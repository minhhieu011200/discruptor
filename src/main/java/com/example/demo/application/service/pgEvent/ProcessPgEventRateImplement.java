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

        JsonNode refid = JsonUtils.parseRefId(objectMapper, message.path("refid").asText());
        if (refid == null)
            return;

        String imtcode = JsonUtils.extractText(refid, "IMTCODE");
        if (imtcode == null)
            return;

        double bid = JsonUtils.extractDouble(refid, "BUYRATE");
        double ask = JsonUtils.extractDouble(refid, "SELLRATE");

        SymbolEntity symbol = symbolRepository.get(imtcode);

        if (symbol == null) {
            symbol = new SymbolEntity();
            symbol.setImtcode(imtcode);
        }

        symbol.setBid(bid);
        symbol.setAsk(ask);
        symbol.setVersion(symbol.getVersion() + 1);

        // ALWAYS write back (ChronicleMap không tự update object)
        symbolRepository.set(imtcode, symbol);
    }
}
