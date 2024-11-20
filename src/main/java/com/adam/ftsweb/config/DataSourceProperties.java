package com.adam.ftsweb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "datasource")
@Data
public class DataSourceProperties {

    private DataSource master;
    private DataSource slave;
    private String driverClassName;
    private int initialSize;
    private int maxActive;
    private int maxIdle;
    private int minIdle;
    private long maxWait;

    @Data
    public static class DataSource {
        private String url;
        private String username;
        private String password;
    }

}
