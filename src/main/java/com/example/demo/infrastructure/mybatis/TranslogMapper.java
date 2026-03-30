package com.example.demo.infrastructure.mybatis;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.domain.entity.TranslogEntity;

@Mapper
public interface TranslogMapper {
    @Insert({
            "<script>",
            "INSERT INTO translog (symbol, price, volume, timestamp) VALUES ",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.symbol}, #{item.price}, #{item.volume}, #{item.timestamp})",
            "</foreach>",
            "</script>"
    })
    void insertBatch(@Param("list") List<TranslogEntity> list);
}
