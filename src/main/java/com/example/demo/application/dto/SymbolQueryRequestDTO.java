package com.example.demo.application.dto;

import java.util.List;

public record SymbolQueryRequestDTO(List<String> ids, String accountId) {
}
