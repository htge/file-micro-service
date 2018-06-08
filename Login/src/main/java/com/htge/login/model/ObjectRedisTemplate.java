package com.htge.login.model;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

public class ObjectRedisTemplate extends RedisTemplate<String, Object> {

    public ObjectRedisTemplate() {
        RedisSerializer<String> stringSerializer = new StringRedisSerializer();
        this.setKeySerializer(stringSerializer);
        this.setValueSerializer(null);
        this.setHashKeySerializer(stringSerializer);
        this.setHashValueSerializer(null);
    }

    public ObjectRedisTemplate(RedisConnectionFactory connectionFactory) {
        this();
        this.setConnectionFactory(connectionFactory);
        this.afterPropertiesSet();
    }
}
