package com.founderlink.startup.config;

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

    @Value("${rabbitmq.startup.queue}")
    private String startupQueue;

    @Value("${rabbitmq.startup.routing-key}")
    private String startupRoutingKey;

    @Value("${rabbitmq.startup.deleted.routing-key}")
    private String startupDeletedRoutingKey;

    @Value("${rabbitmq.investment.deleted.queue}")
    private String investmentDeletedQueue;

    @Value("${rabbitmq.team.deleted.queue}")
    private String teamDeletedQueue;

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
    public Queue startupQueue() {
        return new Queue(startupQueue, true);
    }

    @Bean
    public Queue investmentDeletedQueue() {
        return new Queue(
                investmentDeletedQueue, true);
    }

    @Bean
    public Queue teamDeletedQueue() {
        return new Queue(teamDeletedQueue, true);
    }

    // ─────────────────────────────────────────
    // BINDINGS
    // ─────────────────────────────────────────
    @Bean
    public Binding startupCreatedBinding() {
        return BindingBuilder
                .bind(startupQueue())
                .to(founderLinkExchange())
                .with(startupRoutingKey);
    }

    @Bean
    public Binding investmentDeletedBinding() {
        return BindingBuilder
                .bind(investmentDeletedQueue())
                .to(founderLinkExchange())
                .with(startupDeletedRoutingKey);
    }

    @Bean
    public Binding teamDeletedBinding() {
        return BindingBuilder
                .bind(teamDeletedQueue())
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