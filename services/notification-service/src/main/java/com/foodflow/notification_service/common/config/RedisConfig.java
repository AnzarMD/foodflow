package com.foodflow.notification_service.common.config;

import com.foodflow.notification_service.notification.OrderUpdateListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {

    public static final String ORDER_UPDATES_CHANNEL = "order-updates";
    // Must exactly match the channel name Order Service publishes to.

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            OrderUpdateListener orderUpdateListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                orderUpdateListener,
                new PatternTopic(ORDER_UPDATES_CHANNEL)
                // ↑ PatternTopic subscribes to the Redis channel by name.
                //   This is the subscriber side of Redis pub/sub.
                //   When Order Service does redisTemplate.convertAndSend("order-updates", msg),
                //   this container receives it and calls orderUpdateListener.onMessage().
        );
        return container;
        // ↑ This container runs in its own thread, permanently listening
        //   to Redis. It's completely separate from the HTTP/WebSocket threads.
        //   When a message arrives, it dispatches to OrderUpdateListener.
    }
}