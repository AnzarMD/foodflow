package com.foodflow.order_service.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ── Constants ────────────────────────────────────────────────────────────
    public static final String EXCHANGE = "foodflow.events";
    public static final String RESTAURANT_QUEUE = "restaurant-service-queue";
    public static final String ORDER_QUEUE = "order-service-queue";
    public static final String ROUTING_KEY_ORDER_PLACED = "order.placed";
    public static final String ROUTING_KEY_ORDER_ACCEPTED = "order.accepted";
    public static final String ROUTING_KEY_ORDER_REJECTED = "order.rejected";

    // ── Exchange ─────────────────────────────────────────────────────────────

    @Bean
    public TopicExchange foodflowExchange() {
        return ExchangeBuilder
                .topicExchange(EXCHANGE)
                .durable(true)   // ← survives RabbitMQ restarts
                .build();
    }

    // ── Queues ───────────────────────────────────────────────────────────────

    @Bean
    public Queue restaurantServiceQueue() {
        return QueueBuilder
                .durable(RESTAURANT_QUEUE)  // ← durable: messages survive restart
                .build();
    }

    @Bean
    public Queue orderServiceQueue() {
        return QueueBuilder
                .durable(ORDER_QUEUE)
                .build();
    }

    // ── Bindings ─────────────────────────────────────────────────────────────

    // Restaurant Service listens for order.placed
    @Bean
    public Binding restaurantOrderPlacedBinding() {
        return BindingBuilder
                .bind(restaurantServiceQueue())
                .to(foodflowExchange())
                .with(ROUTING_KEY_ORDER_PLACED);
        // ↑ "When a message arrives at foodflow.events with key order.placed,
        //    put it in restaurant-service-queue"
    }

    // Order Service will listen for order.accepted and order.rejected (Day 9)
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

    // ── Message Converter ────────────────────────────────────────────────────

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
        // ↑ Tells RabbitTemplate to serialize Java objects as JSON
        //   and deserialize JSON back to Java objects.
        //   Without this, Spring uses Java serialization (binary) by default,
        //   which is brittle and unreadable in the RabbitMQ UI.
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        // ↑ Wire the JSON converter into the template so every
        //   convertAndSend() call automatically serializes to JSON
        return template;
    }
}