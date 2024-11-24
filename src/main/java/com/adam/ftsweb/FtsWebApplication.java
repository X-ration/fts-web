package com.adam.ftsweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FtsWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(FtsWebApplication.class, args);
    }

}
