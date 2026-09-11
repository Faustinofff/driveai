package com.driveai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DriveaiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DriveaiApplication.class, args);
    }
}