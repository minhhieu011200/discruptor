package com.example.demo.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.impossibl.postgres.jdbc.PGDataSource;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "pg.datasource")
@Data
public class PgEventDataSourceConfig {
    private String host;
    private int port;
    private String database;
    private String user;
    private String password;
    private String applicationName;

    @Bean(name = "pgEventDataSource")
    public com.impossibl.postgres.jdbc.PGDataSource pgEventDataSource() {

        PGDataSource ds = new PGDataSource();
        ds.setServerName(host);
        ds.setPortNumber(port);
        ds.setDatabaseName(database);
        ds.setUser(user);
        ds.setPassword(password);
        ds.setApplicationName(applicationName);
        return ds;
    }
}
