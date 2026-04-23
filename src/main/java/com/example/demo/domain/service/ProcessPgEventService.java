package com.example.demo.domain.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface ProcessPgEventService {
    void process(JsonNode message);
}
