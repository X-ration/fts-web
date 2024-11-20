package com.adam.ftsweb.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

//@Configuration
public class DynamicDataSourceConfig {

    @Value("${datasource.master.url}")
    private String masterUrl;
    @Value("${datasource.master.username}")
    private String masterUsername;
    @Value("${datasource.master.password}")
    private String masterPassword;
    @Value("${datasource.slave.url}")
    private String slaveUrl;
    @Value("${datasource.slave.username}")
    private String slaveUsername;
    @Value("${datasource.slave.password}")
    private String slavePassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        DynamicDataSource dataSource = new DynamicDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DynamicDataSourceKeyHolder.MASTER, masterDataSource());
        targetDataSources.put(DynamicDataSourceKeyHolder.SLAVE, slaveDataSource());
        dataSource.setTargetDataSources(targetDataSources);
        dataSource.setDefaultTargetDataSource(slaveDataSource());
        return dataSource;
    }

    @Bean
    public DataSource masterDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(masterUrl);
        config.setUsername(masterUsername);
        config.setPassword(masterPassword);
        commonPoolConfig(config);
        return new HikariDataSource(config);
    }

    @Bean
    public DataSource slaveDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(slaveUrl);
        config.setUsername(slaveUsername);
        config.setPassword(slavePassword);
        commonPoolConfig(config);
        return new HikariDataSource(config);
    }

    private void commonPoolConfig(HikariConfig config) {
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5000);
    }

}
