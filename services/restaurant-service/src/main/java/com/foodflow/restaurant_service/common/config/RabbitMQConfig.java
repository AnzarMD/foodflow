package com.foodflow.restaurant_service.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Constants — same names as order-service. Must match exactly.
    public static final String EXCHANGE = "foodflow.events";
    public static final String RESTAURANT_QUEUE = "restaurant-service-queue";
    public static final String ORDER_QUEUE = "order-service-queue";
    public static final String ROUTING_KEY_ORDER_PLACED   = "order.placed";
    public static final String ROUTING_KEY_ORDER_ACCEPTED = "order.accepted";
    public static final String ROUTING_KEY_ORDER_REJECTED = "order.rejected";

    @Bean
    public TopicExchange foodflowExchange() {
        return ExchangeBuilder
                .topicExchange(EXCHANGE)
                .durable(true)
                .build();
        // Declaring the same exchange as order-service is safe —
        // RabbitMQ is idempotent: declaring an existing exchange with
        // the same properties is a no-op. It does NOT create a duplicate.
    }

    @Bean
    public Queue restaurantServiceQueue() {
        return QueueBuilder
                .durable(RESTAURANT_QUEUE)
                .build();
        // Again idempotent — same queue name, same durability.
        // If order-service already created it, this is a no-op.
    }

    @Bean
    public Queue orderServiceQueue() {
        return QueueBuilder.durable(ORDER_QUEUE).build();
    }

    @Bean
    public Binding restaurantOrderPlacedBinding() {
        return BindingBuilder
                .bind(restaurantServiceQueue())
                .to(foodflowExchange())
                .with(ROUTING_KEY_ORDER_PLACED);
        // This is the binding that routes "order.placed" messages
        // into restaurant-service-queue — the queue THIS service listens to.
    }

    @Bean
    public Binding orderAcceptedBinding() {
        return BindingBuilder
                .bind(orderServiceQueue())
                .to(foodflowExchange())
                .with(ROUTING_KEY_ORDER_ACCEPTED);
    }

    @Bean
    public Binding orderRejectedBinding() {
        return BindingBuilder
                .bind(orderServiceQueue())
                .to(foodflowExchange())
                .with(ROUTING_KEY_ORDER_REJECTED);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
        // CRITICAL: must use the same converter as order-service.
        // Order Service serialized the message as JSON.
        // This converter deserializes that JSON back into a Java object here.
        // If converters don't match, deserialization fails.
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
        // Used by RestaurantService to publish order.accepted / order.rejected
    }
}