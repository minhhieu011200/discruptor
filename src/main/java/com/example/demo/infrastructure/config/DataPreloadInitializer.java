package com.example.demo.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.example.demo.application.service.DataPreloadService;

/**
 * Startup initializer component
 * Orchestrates the data preloading process on application startup
 */
@Slf4j
@Component
public class DataPreloadInitializer implements ApplicationRunner {

    private final DataPreloadService dataPreloadService;

    public DataPreloadInitializer(DataPreloadService dataPreloadService) {
        this.dataPreloadService = dataPreloadService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // dataPreloadService.preloadAllData();
    }
}
