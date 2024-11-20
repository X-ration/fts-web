package com.adam.ftsweb.config;

import com.adam.ftsweb.datasource.DynamicDataSource;
import com.adam.ftsweb.datasource.DynamicDataSourceKeyHolder;
import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionManager;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
@MapperScan("com.adam.ftsweb.mapper")
public class MyBatisConfig {

    @Bean
    public SqlSessionFactoryBean sqlSessionFactory(DataSourceProperties dataSourceProperties) {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource(dataSourceProperties));
        return sqlSessionFactoryBean;
    }

    @Bean
    public SqlSessionTemplate sqlSession(@Autowired SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean
    public TransactionManager transactionManager(@Autowired DataSource dataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(dataSource);
        return transactionManager;
    }

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties dataSourceProperties) {
        DynamicDataSource dataSource = new DynamicDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        DataSource masterDataSource = masterDataSource(dataSourceProperties),
                slaveDataSource = slaveDataSource(dataSourceProperties);
        targetDataSources.put(DynamicDataSourceKeyHolder.MASTER, masterDataSource);
        targetDataSources.put(DynamicDataSourceKeyHolder.SLAVE, slaveDataSource);
        dataSource.setTargetDataSources(targetDataSources);
        dataSource.setDefaultTargetDataSource(slaveDataSource);
        return dataSource;
    }

    @Bean
    public DruidDataSource masterDataSource(DataSourceProperties dataSourceProperties) {
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setUrl(dataSourceProperties.getMaster().getUrl());
        druidDataSource.setUsername(dataSourceProperties.getMaster().getUsername());
        druidDataSource.setPassword(dataSourceProperties.getMaster().getPassword());
        postProcessDataSource(druidDataSource, dataSourceProperties);
        return druidDataSource;
    }

    @Bean
    public DruidDataSource slaveDataSource(DataSourceProperties dataSourceProperties) {
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setUrl(dataSourceProperties.getSlave().getUrl());
        druidDataSource.setUsername(dataSourceProperties.getSlave().getUsername());
        druidDataSource.setPassword(dataSourceProperties.getSlave().getPassword());
        postProcessDataSource(druidDataSource, dataSourceProperties);
        return druidDataSource;
    }

    private void postProcessDataSource(DruidDataSource druidDataSource, DataSourceProperties dataSourceProperties) {
        druidDataSource.setDriverClassName(dataSourceProperties.getDriverClassName());
        druidDataSource.setInitialSize(dataSourceProperties.getInitialSize());
        druidDataSource.setMaxActive(dataSourceProperties.getMaxActive());
//        druidDataSource.setMaxIdle(dataSourceProperties.getMaxIdle());
        druidDataSource.setMinIdle(dataSourceProperties.getMinIdle());
        druidDataSource.setMaxWait(dataSourceProperties.getMaxWait());
        try {
            druidDataSource.init();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
