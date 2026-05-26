package com.example.internmanager;

import com.example.internmanager.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class InternManagerBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(InternManagerBackendApplication.class, args);
    }
}
