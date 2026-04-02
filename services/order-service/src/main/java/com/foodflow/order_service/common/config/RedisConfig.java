package com.foodflow.order_service.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    public static final String ORDER_UPDATES_CHANNEL = "order-updates";
    // Constant for the Redis pub/sub channel name.
    // Notification Service will subscribe to this exact channel name on Day 10.
    // Using a constant avoids typos — one wrong character and messages go nowhere.

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        // Both key and value serialized as plain strings.
        // We'll publish JSON strings manually (not Java objects) — this keeps
        // the message format simple and readable for the Notification Service.
        return template;
    }
}