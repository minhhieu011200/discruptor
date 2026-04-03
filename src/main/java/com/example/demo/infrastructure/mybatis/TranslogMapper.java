package com.example.demo.infrastructure.mybatis;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.domain.entity.TranslogEntity;

@Mapper
public interface TranslogMapper {
    void insertBatch(@Param("list") List<TranslogEntity> list);
}
