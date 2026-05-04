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
        JsonNode refidNode = JsonUtils.parseRefId(objectMapper, message.path("refid").asText());
        if (refidNode == null)
            return;

        String cfid = JsonUtils.extractText(refidNode, "CIFID");
        String imtcode = JsonUtils.extractText(refidNode, "IMTCODE");
        Double marginBuyObj = JsonUtils.extractDouble(refidNode, "MARGINBUY");
        Double marginSellObj = JsonUtils.extractDouble(refidNode, "MARGINSELL");
        double marginBuy = marginBuyObj != null ? marginBuyObj : 0.0;
        double marginSell = marginSellObj != null ? marginSellObj : 0.0;
        if (cfid == null || imtcode == null) {
            log.error("Missing CIFID or IMTCODE: {}", refidNode);
            return;
        }
        String accountKey = cfid + imtcode;

        AccountEntity account = accountRepository.get(accountKey);
        if (account == null) {
            account = new AccountEntity();

            account.setCifid(cfid);
            account.setImtcode(imtcode);
            account.setMarginBuy(marginBuy);
            account.setMarginSell(marginSell);

            account.setVersion();
            accountRepository.set(accountKey, account);
            return;
        }
        account.setMarginBuy(marginBuy);
        account.setMarginSell(marginSell);
        account.setVersion();
    }
}