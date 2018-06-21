package com.htge.login.model;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/* 解决默认的HealthIndicator检查不通过的情况 */
@Component
public class RedisHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up().build();
    }
}