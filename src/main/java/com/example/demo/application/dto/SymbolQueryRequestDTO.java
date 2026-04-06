package com.example.demo.application.dto;

import java.util.List;

import lombok.Data;

@Data
public class SymbolQueryRequestDTO {
    private List<String> ids;
    private String accountId;
}
