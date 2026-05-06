package com.example.demo.application.service.pgEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import com.example.demo.application.annotation.Measured;
import com.example.demo.application.annotation.PgEventType;
import com.example.demo.domain.entity.AccountEntity;
import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.repository.AccountRepository;
import com.example.demo.domain.service.ProcessPgEventService;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.demo.application.utils.JsonUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@PgEventType("CF_ENGINE")
@Component
public class ProcessPgEventCFImplement implements ProcessPgEventService {

    private final ObjectMapper objectMapper;
    private final AccountRepository accountRepository;

    public ProcessPgEventCFImplement(AccountRepository accountRepository, ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Measured(value = "pg.event.cf", description = "Time to process CF event")
    public void process(JsonNode message) {

        JsonNode refid = JsonUtils.parseRefId(objectMapper, message.path("refid").asText());
        if (refid == null)
            return;

        String cfid = JsonUtils.extractText(refid, "CIFID");
        String imtcode = JsonUtils.extractText(refid, "IMTCODE");
        if (cfid == null || imtcode == null) {
            log.error("Missing CIFID or IMTCODE: {}", refid);
            return;
        }

        double marginBuy = JsonUtils.extractDouble(refid, "MARGINBUY");
        double marginSell = JsonUtils.extractDouble(refid, "MARGINSELL");

        String accountKey = cfid + imtcode;
        AccountEntity acc = accountRepository.get(accountKey);

        if (acc == null) {
            acc = new AccountEntity();
            acc.setCifid(cfid);
            acc.setImtcode(imtcode);
        }

        acc.setMarginBuy(marginBuy);
        acc.setMarginSell(marginSell);

        // Always put back (ChronicleMap & concurrency-safe)
        accountRepository.set(accountKey, acc);
    }
}