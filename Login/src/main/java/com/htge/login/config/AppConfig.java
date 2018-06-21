package com.htge.login.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@EnableAutoConfiguration
@ImportResource(locations={"classpath:login.xml","classpath:jdbc.xml", "classpath:redis.xml", "classpath:shiro.xml", "classpath:rabbitmq.xml"})
public class AppConfig {

}
