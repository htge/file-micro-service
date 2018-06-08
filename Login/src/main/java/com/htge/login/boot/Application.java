package com.htge.login.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.htge.login")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}