package com.example.demo.infrastructure.mybatis;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.example.demo.domain.entity.SymbolEntity;

@Mapper
public interface SymbolMapper {
    int batchUpsert(@Param("symbols") List<SymbolEntity> symbols);

    List<SymbolEntity> getAllSymbols();
}