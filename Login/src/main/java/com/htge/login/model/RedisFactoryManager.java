package com.htge.login.model;

import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

public class RedisFactoryManager {
    private JedisConnectionFactory jedisConnectionFactory;

    public void setJedisConnectionFactory(JedisConnectionFactory jedisConnectionFactory) {
        this.jedisConnectionFactory = jedisConnectionFactory;
    }

    RedisTemplate<String, Object> getTemplate(int database) {
        JedisConnectionFactory factory = new JedisConnectionFactory();
        BeanUtils.copyProperties(jedisConnectionFactory, factory);
        factory.setShardInfo(null);
        factory.setDatabase(database);
        factory.afterPropertiesSet();
        return new ObjectRedisTemplate(factory);
    }
}
