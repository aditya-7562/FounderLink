package com.founderlink.team.config;

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

    // CONSTANTS
 
    public static final String TEAM_QUEUE       = "team.queue";
    public static final String TEAM_EXCHANGE    = "founderlink.exchange";
    public static final String TEAM_ROUTING_KEY = "team.invite.sent";
    

    // STARTUP DELETED CONSTANTS           
    
    public static final String STARTUP_DELETED_QUEUE
            = "team.startup.deleted.queue";
    public static final String STARTUP_EXCHANGE
            = "founderlink.exchange";
    public static final String STARTUP_DELETED_ROUTING_KEY
            = "startup.deleted";

    // QUEUE
    
    @Bean
    public Queue teamQueue() {
        return new Queue(TEAM_QUEUE, true);
    }
    
    // STARTUP DELETED QUEUE
    
    @Bean
    public Queue startupDeletedQueue() {
        return new Queue(
                STARTUP_DELETED_QUEUE, true);
    }
    // EXCHANGE

    @Bean
    public DirectExchange teamExchange() {
        return new DirectExchange(TEAM_EXCHANGE);
    }
    
   
    // STARTUP EXCHANGE                  
    // Same exchange as Startup Service

    @Bean
    public DirectExchange startupExchange() {
        return new DirectExchange(STARTUP_EXCHANGE);
    }

    // BINDING
    
    @Bean
    public Binding teamBinding() {
        return BindingBuilder
                .bind(teamQueue())
                .to(teamExchange())
                .with(TEAM_ROUTING_KEY);
    }
    
    @Bean
    public Binding startupDeletedBinding() {
        return BindingBuilder
                .bind(startupDeletedQueue())
                .to(startupExchange())
                .with(STARTUP_DELETED_ROUTING_KEY);
    }
    
    // MESSAGE CONVERTER
    // converts Java object → JSON automatically
    
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // RABBIT TEMPLATE
    
    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate =
                new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}