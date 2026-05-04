package com.example.demo.application.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtils {

    private JsonUtils() {
        // Prevent instantiation
    }

    public static JsonNode parseRefId(ObjectMapper objectMapper, String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            log.error("Cannot parse refid JSON: {}", raw, e);
            return null;
        }
    }

    public static String extractText(JsonNode node, String field) {
        JsonNode f = node.get(field);
        return (f != null && !f.isNull()) ? f.asText() : null;
    }

    public static Double extractDouble(JsonNode node, String field) {
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
