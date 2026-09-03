package com.agtech.cloudbedsbatchcr.config;

import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

@Configuration
public class TimeZoneConfig {

    @PostConstruct
    public void init() {
        // Configura la zona horaria en GMT-6
        TimeZone timeZone = TimeZone.getTimeZone("GMT-6");

        // Establece la zona horaria predeterminada para la JVM
        TimeZone.setDefault(timeZone);
    }
}
