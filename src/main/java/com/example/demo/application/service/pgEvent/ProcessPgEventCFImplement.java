package com.example.demo.application.service.pgEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import com.example.demo.application.annotation.PgEventType;
import com.example.demo.domain.entity.AccountEntity;
import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.repository.AccountRepository;
import com.example.demo.domain.service.ProcessPgEventService;
import com.fasterxml.jackson.databind.JsonNode;

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
    public void process(JsonNode message) {
        log.info("Process CF event: {}", message);
        JsonNode refidNode = parseRefId(message.path("refid").asText());
        log.info("Refid node: {}", refidNode);
        if (refidNode == null)
            return;

        String cfid = extractText(refidNode, "CIFID");
        String imtcode = extractText(refidNode, "IMTCODE");
        double marginBuy = extractDouble(refidNode, "MARGINBUY");
        double marginSell = extractDouble(refidNode, "MARGINSELL");
        if (cfid == null || imtcode == null) {
            log.error("Missing CIFID or IMTCODE: {}", refidNode);
            return;
        }
        String accountKey = cfid + imtcode;

        AccountEntity account = accountRepository.get(accountKey);
        log.info("Creating new account with key: {} {}", accountKey, account);
        // Nếu chưa có thì tạo mới + set version luôn
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

        // Nếu đã có thì update
        account.setMarginBuy(marginBuy);
        account.setMarginSell(marginSell);
        account.setVersion(); // 🔥 set version khi update
    }

    private JsonNode parseRefId(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            log.error("Cannot parse refid JSON: {}", raw, e);
            return null;
        }
    }

    private String extractText(JsonNode node, String field) {
        JsonNode f = node.get(field);
        return (f != null && !f.isNull()) ? f.asText() : null;
    }

    private Double extractDouble(JsonNode node, String field) {
        JsonNode f = node.get(field);
        if (f == null || f.isNull())
            return null;

        // Nếu là số → dùng trực tiếp
        if (f.isNumber()) {
            return f.asDouble();
        }

        // Nếu là string → parse
        if (f.isTextual()) {
            String txt = f.asText().trim();
            if (txt.isEmpty())
                return null;

            try {
                return Double.parseDouble(txt);
            } catch (NumberFormatException e) {
                log.warn("Cannot parse double from field {} with value {}", field, txt);
                return null;
            }
        }

        return null;
    }
}