package com.example.demo.application.service;

import org.springframework.stereotype.Service;

import com.example.demo.domain.entity.SymbolEntity;
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
    public SymbolEntity process(String data) {
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
            SymbolEntity entity = SymbolEntity.builder()
                    .imtcode(msgNode.get("pid").asText())
                    .bid(Math.round(Double.parseDouble(msgNode.get("bid").asText().replace(",", "")) * 10000))
                    .ask(Math.round(Double.parseDouble(msgNode.get("ask").asText().replace(",", "")) * 10000))
                    .build();
            return entity;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
