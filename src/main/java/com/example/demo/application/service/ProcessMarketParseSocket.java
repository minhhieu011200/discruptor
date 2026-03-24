package com.example.demo.application.service;

import org.springframework.stereotype.Service;

import com.example.demo.domain.entity.MarketEntity;
import com.example.demo.domain.service.ProcessMarketParseService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ProcessMarketParseSocket implements ProcessMarketParseService<String> {

    private final ObjectMapper mapper;

    public ProcessMarketParseSocket(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public MarketEntity process(String data) {
        try {

            // 1️⃣ Strip outer a[" ... "] safely
            String element = data.substring(data.indexOf('[') + 1, data.lastIndexOf(']'));
            if (element.startsWith("\"") && element.endsWith("\"")) {
                element = element.substring(1, element.length() - 1);
            }

            // 2️⃣ Unescape JSON inside
            element = element.replace("\\\"", "\"").replace("\\\\", "\\");

            // 3️⃣ Parse outer JSON
            JsonNode root = mapper.readTree(element);
            JsonNode messageNode = root.get("message");
            if (messageNode == null) {
                throw new IllegalArgumentException("No 'message' field found in: " + element);
            }
            String message = messageNode.asText();

            // 4️⃣ Split by '::{' to get inner JSON
            int idx = message.indexOf("::");
            if (idx == -1) {
                throw new IllegalArgumentException("Input does not contain '::': " + idx);
            }

            String jsonPart = message.substring(idx + 2);

            // 5️⃣ Parse inner JSON
            JsonNode msgNode = mapper.readTree(jsonPart);

            // 6️⃣ Populate MarketEntity
            MarketEntity entity = new MarketEntity();

            // pid is string, parse safely
            entity.setSymbol(Integer.parseInt(msgNode.get("pid").asText()));

            // last_numeric is safe number
            entity.setLast(Math.round(msgNode.get("last_numeric").asDouble() * 10000));

            // bid/ask may have commas, remove them
            String bidStr = msgNode.get("bid").asText().replace(",", "");
            String askStr = msgNode.get("ask").asText().replace(",", "");

            entity.setBid(Math.round(Double.parseDouble(bidStr) * 10000));
            entity.setAsk(Math.round(Double.parseDouble(askStr) * 10000));

            return entity;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
