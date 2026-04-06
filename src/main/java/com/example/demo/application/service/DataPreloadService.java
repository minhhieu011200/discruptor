package com.example.demo.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.example.demo.domain.entity.AccountEntity;
import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.repository.AccountRepository;
import com.example.demo.domain.repository.SymbolRepository;
import com.example.demo.infrastructure.mybatis.AccountMapper;
import com.example.demo.infrastructure.mybatis.SymbolMapper;

import io.micrometer.common.util.StringUtils;

import java.util.List;

/**
 * Service layer for data preloading logic
 * Handles loading data from MyBatis into cache
 */
@Slf4j
@Service
public class DataPreloadService {

    private final SymbolMapper symbolMapper;
    private final AccountMapper accountMapper;
    private final SymbolRepository symbolRepository;
    private final AccountRepository accountRepository;

    public DataPreloadService(SymbolMapper symbolMapper,
            AccountMapper accountMapper,
            SymbolRepository symbolRepository,
            AccountRepository accountRepository) {
        this.symbolMapper = symbolMapper;
        this.accountMapper = accountMapper;
        this.symbolRepository = symbolRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Load all symbols from database and store in cache
     */
    public void loadSymbols() {
        log.debug("Loading symbols from database...");
        try {
            List<SymbolEntity> symbols = symbolMapper.getAllSymbols();

            if (symbols == null || symbols.isEmpty()) {
                log.warn("No symbols found in database");
                return;
            }

            int loadedCount = 0;
            for (SymbolEntity symbol : symbols) {
                if (!StringUtils.isBlank(symbol.getImtcode())) {
                    symbolRepository.set(symbol.getImtcode(), symbol);
                    loadedCount++;
                }
            }

            log.info("Successfully loaded {} symbols into cache", loadedCount);
        } catch (Exception e) {
            log.error("Error loading symbols into cache", e);
            throw e;
        }
    }

    /**
     * Load account data from database and store in cache
     */
    public void loadAccounts() {
        log.debug("Loading accounts from database...");
        try {
            List<AccountEntity> accounts = accountMapper.getAllAccount();

            if (accounts == null || accounts.isEmpty()) {
                log.warn("No accounts found in database");
                return;
            }

            int loadedCount = 0;
            for (AccountEntity account : accounts) {
                if (!StringUtils.isBlank(account.getImtcode())) {
                    accountRepository.set(account.getCifid() + account.getImtcode(), account);
                    loadedCount++;
                }
            }

            log.info("Successfully loaded {} account into cache", loadedCount);
        } catch (Exception e) {
            log.error("Error loading account data into cache", e);
            throw e;
        }
    }

    /**
     * Load all data into cache (symbols and accounts)
     */
    public void preloadAllData() {
        log.info("Starting data preload initialization...");
        try {
            loadSymbols();
            loadAccounts();
            log.info("Data preload completed successfully");
        } catch (Exception e) {
            log.error("Error during data preload initialization", e);
            throw e;
        }
    }
}
