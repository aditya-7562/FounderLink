package com.founderlink.investment.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String INVESTMENT_QUEUE    = "investment.queue";
    public static final String INVESTMENT_EXCHANGE = "founderlink.exchange";
    public static final String INVESTMENT_ROUTING_KEY = "investment.created";
    
    
    public static final String STARTUP_DELETED_QUEUE
            = "investment.startup.deleted.queue";
    public static final String STARTUP_EXCHANGE
            = "founderlink.exchange";
    public static final String STARTUP_DELETED_ROUTING_KEY
            = "startup.deleted";

    @Bean
    public Queue investmentQueue() {
        return new Queue(INVESTMENT_QUEUE, true);
    }
    
    @Bean
    public Queue startupDeletedQueue() {
        return new Queue(
                STARTUP_DELETED_QUEUE, true);
    }
    

    // STARTUP EXCHANGE
    // Same exchange as Startup Service
    
    @Bean
    public DirectExchange startupExchange() {
        return new DirectExchange(STARTUP_EXCHANGE);
    }

   
    @Bean
    public DirectExchange investmentExchange() {
        return new DirectExchange(INVESTMENT_EXCHANGE);
    }
    
    
    // STARTUP DELETED BINDING
    
    @Bean
    public Binding startupDeletedBinding() {
        return BindingBuilder
                .bind(startupDeletedQueue())
                .to(startupExchange())
                .with(STARTUP_DELETED_ROUTING_KEY);
    }

    @Bean
    public Binding investmentBinding() {
        return BindingBuilder
                .bind(investmentQueue())
                .to(investmentExchange())
                .with(INVESTMENT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}