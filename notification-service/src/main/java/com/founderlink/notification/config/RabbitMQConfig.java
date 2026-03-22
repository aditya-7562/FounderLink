package com.founderlink.notification.config;

import org.springframework.amqp.core.*;
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

    @Value("${rabbitmq.queue.startup}")
    private String startupQueue;

    @Value("${rabbitmq.queue.investment}")
    private String investmentQueue;

    @Value("${rabbitmq.queue.team}")
    private String teamQueue;

    @Value("${rabbitmq.queue.messaging}")
    private String messagingQueue;

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(exchange);
    }

    @Bean
    public Queue startupQueue() {
        return new Queue(startupQueue, true);
    }

    @Bean
    public Queue investmentQueue() {
        return new Queue(investmentQueue, true);
    }

    @Bean
    public Queue teamQueue() {
        return new Queue(teamQueue, true);
    }

    @Bean
    public Queue messagingQueue() {
        return new Queue(messagingQueue, true);
    }

    @Bean
    public Binding startupBinding(Queue startupQueue, DirectExchange exchange) {
        return BindingBuilder.bind(startupQueue).to(exchange).with("startup.created");
    }

    @Bean
    public Binding investmentBinding(Queue investmentQueue, DirectExchange exchange) {
        return BindingBuilder.bind(investmentQueue).to(exchange).with("investment.created");
    }

    @Bean
    public Binding teamBinding(Queue teamQueue, DirectExchange exchange) {
        return BindingBuilder.bind(teamQueue).to(exchange).with("team.invite.sent");
    }

    @Bean
    public Binding messagingBinding(Queue messagingQueue, DirectExchange exchange) {
        return BindingBuilder.bind(messagingQueue).to(exchange).with("message.sent");
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
