package com.example.demo.infrastructure.mybatis;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.domain.entity.AccountEntity;

@Mapper
public interface AccountMapper {
    List<AccountEntity> getAllAccount();
}
