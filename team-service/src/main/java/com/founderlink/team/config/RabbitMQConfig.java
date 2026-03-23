package com.founderlink.team.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.team.queue}")
    private String teamQueue;

    @Value("${rabbitmq.team.routing-key}")
    private String teamRoutingKey;

    @Value("${rabbitmq.startup.deleted.queue}")
    private String startupDeletedQueue;

    @Value("${rabbitmq.startup.deleted.routing-key}")
    private String startupDeletedRoutingKey;

    // ─────────────────────────────────────────
    // SINGLE EXCHANGE
    // ─────────────────────────────────────────
    @Bean
    public DirectExchange founderLinkExchange() {
        return new DirectExchange(exchange);
    }

    // ─────────────────────────────────────────
    // QUEUES
    // ─────────────────────────────────────────
    @Bean
    public Queue teamQueue() {
        return new Queue(teamQueue, true);
    }

    @Bean
    public Queue startupDeletedQueue() {
        return new Queue(startupDeletedQueue, true);
    }

    // ─────────────────────────────────────────
    // BINDINGS
    // ─────────────────────────────────────────
    @Bean
    public Binding teamInviteBinding() {
        return BindingBuilder
                .bind(teamQueue())
                .to(founderLinkExchange())
                .with(teamRoutingKey);
    }

    @Bean
    public Binding startupDeletedBinding() {
        return BindingBuilder
                .bind(startupDeletedQueue())
                .to(founderLinkExchange())
                .with(startupDeletedRoutingKey);
    }

    // ─────────────────────────────────────────
    // MESSAGE CONVERTER
    // ─────────────────────────────────────────
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ─────────────────────────────────────────
    // RABBIT TEMPLATE
    // ─────────────────────────────────────────
    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate =
                new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(
                messageConverter());
        return rabbitTemplate;
    }
}