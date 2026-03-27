package com.founderlink.auth.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PASSWORD_RESET_QUEUE = "password-reset-queue";
    public static final String NOTIFICATION_EXCHANGE = "notification-exchange";
    public static final String PASSWORD_RESET_ROUTING_KEY = "password.reset";

    @Bean
    public Queue passwordResetQueue() {
        return new Queue(PASSWORD_RESET_QUEUE, true);
    }

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Binding passwordResetBinding(Queue passwordResetQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(passwordResetQueue)
                .to(notificationExchange)
                .with(PASSWORD_RESET_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
