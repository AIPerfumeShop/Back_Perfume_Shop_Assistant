package com.example.spring_boot_project_api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BakongProperties.class)
public class BakongConfig {

}