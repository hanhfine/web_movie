package com.webphim.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

@Configuration
public class RedisConfig {
    @Bean
    LettuceConnectionFactory redisConnectionFactory(
            org.springframework.boot.autoconfigure.data.redis.RedisProperties properties) {
        var config = new RedisStandaloneConfiguration(properties.getHost(), properties.getPort());
        if (StringUtils.hasText(properties.getPassword())) {
            config.setPassword(properties.getPassword());
        }
        return new LettuceConnectionFactory(config);
    }

    @Bean
    StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
