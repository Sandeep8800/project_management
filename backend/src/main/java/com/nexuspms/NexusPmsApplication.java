package com.nexuspms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NexusPmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusPmsApplication.class, args);
    }
}
