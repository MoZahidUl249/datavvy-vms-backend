package com.datavvy.vms.driver.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DriverProperties.class)
public class DriverConfig {
}
