package com.founderlink.investment.config;

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
import io.micrometer.observation.ObservationRegistry;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.investment.queue}")
    private String investmentQueue;

    @Value("${rabbitmq.investment.routing-key}")
    private String investmentRoutingKey;

    @Value("${rabbitmq.startup.deleted.queue}")
    private String startupDeletedQueue;

    @Value("${rabbitmq.startup.deleted.routing-key}")
    private String startupDeletedRoutingKey;

    @Bean
    public DirectExchange founderLinkExchange() {
        return new DirectExchange(exchange);
    }

    @Bean
    public Queue investmentQueue() {
        return new Queue(investmentQueue, true);
    }

    @Bean
    public Queue startupDeletedQueue() {
        return new Queue(startupDeletedQueue, true);
    }

    @Bean
    public Binding investmentCreatedBinding() {
        return BindingBuilder.bind(investmentQueue()).to(founderLinkExchange()).with(investmentRoutingKey);
    }

    @Bean
    public Binding startupDeletedBinding() {
        return BindingBuilder.bind(startupDeletedQueue()).to(founderLinkExchange()).with(startupDeletedRoutingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ── Trace propagation: injects TraceId into outgoing RabbitMQ messages ──
    // and extracts TraceId from incoming RabbitMQ messages automatically
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          ObservationRegistry observationRegistry) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        rabbitTemplate.setObservationEnabled(true);
        return rabbitTemplate;
    }
}
