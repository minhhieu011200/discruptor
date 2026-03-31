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
            "INSERT INTO translog (imtcode,bid,ask,price_tesury,created_time,updated_time) VALUES ",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.imtcode}, #{item.bid}, #{item.ask}, #{item.priceTesury}, #{item.createdTime}, #{item.updatedTime})",
            "</foreach>",
            "</script>"
    })
    void insertBatch(@Param("list") List<TranslogEntity> list);
}
