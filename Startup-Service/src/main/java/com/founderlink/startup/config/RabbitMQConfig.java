package com.founderlink.startup.config;

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

    // STARTUP CREATED CONSTANTS

    public static final String STARTUP_QUEUE
            = "startup.queue";
    public static final String STARTUP_EXCHANGE
            = "founderlink.exchange";
    public static final String STARTUP_ROUTING_KEY
            = "startup.created";


    // STARTUP DELETED CONSTANTS
    public static final String STARTUP_DELETED_ROUTING_KEY
            = "startup.deleted";

    // CONSUMER QUEUES                   
    // Created here so messages are not
    // lost if consumers not started yet

    
    public static final String INVESTMENT_DELETED_QUEUE
            = "investment.startup.deleted.queue";
    public static final String TEAM_DELETED_QUEUE
            = "team.startup.deleted.queue";

    // STARTUP CREATED QUEUE

    @Bean
    public Queue startupQueue() {
        return new Queue(STARTUP_QUEUE, true);
    }


    // INVESTMENT DELETED QUEUE
    
    @Bean
    public Queue investmentDeletedQueue() {
        return new Queue(
                INVESTMENT_DELETED_QUEUE, true);
    }


    // TEAM DELETED QUEUE              
 
    @Bean
    public Queue teamDeletedQueue() {
        return new Queue(
                TEAM_DELETED_QUEUE, true);
    }

    // STARTUP EXCHANGE
  
    @Bean
    public DirectExchange startupExchange() {
        return new DirectExchange(STARTUP_EXCHANGE);
    }

    // STARTUP CREATED BINDING

    @Bean
    public Binding startupBinding() {
        return BindingBuilder
                .bind(startupQueue())
                .to(startupExchange())
                .with(STARTUP_ROUTING_KEY);
    }

 
    // INVESTMENT DELETED BINDING     

    @Bean
    public Binding investmentDeletedBinding() {
        return BindingBuilder
                .bind(investmentDeletedQueue())
                .to(startupExchange())
                .with(STARTUP_DELETED_ROUTING_KEY);
    }


    // TEAM DELETED BINDING              

    @Bean
    public Binding teamDeletedBinding() {
        return BindingBuilder
                .bind(teamDeletedQueue())
                .to(startupExchange())
                .with(STARTUP_DELETED_ROUTING_KEY);
    }


    // MESSAGE CONVERTER

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
        rabbitTemplate.setMessageConverter(
                messageConverter());
        return rabbitTemplate;
    }
}