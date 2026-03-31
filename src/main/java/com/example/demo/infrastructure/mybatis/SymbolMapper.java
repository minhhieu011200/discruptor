package com.example.demo.infrastructure.mybatis;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

import com.example.demo.domain.entity.SymbolEntity;

import io.lettuce.core.dynamic.annotation.Param;

@Mapper
public interface SymbolMapper {

    @Insert({
            "<script>",
            "INSERT INTO symbol (symbol, name, price, updated_time) VALUES ",
            "<foreach collection='symbols' item='s' separator=','>",
            "(#{s.symbol}, #{s.name}, #{s.price}, #{s.updatedTime})",
            "</foreach>",
            "ON DUPLICATE KEY UPDATE",
            "name = VALUES(name),",
            "price = VALUES(price),",
            "updated_time = VALUES(updatedTime)",
            "</script>"
    })
    @Options(useGeneratedKeys = false)
    int batchUpsert(@Param("symbols") List<SymbolEntity> symbols);
}