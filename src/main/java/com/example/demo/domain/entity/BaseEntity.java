package com.example.demo.domain.entity;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
public class BaseEntity {
    private long createdTime;
    private long updatedTime;
}
