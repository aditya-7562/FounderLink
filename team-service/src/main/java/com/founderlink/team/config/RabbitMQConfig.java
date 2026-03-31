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
import io.micrometer.observation.ObservationRegistry;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.team.queue}")
    private String teamQueue;

    @Value("${rabbitmq.team.routing-key}")
    private String teamRoutingKey;

    @Value("${rabbitmq.team.accepted.routing-key}")
    private String teamAcceptedRoutingKey;

    @Value("${rabbitmq.team.rejected.routing-key}")
    private String teamRejectedRoutingKey;

    @Value("${rabbitmq.team.accepted.queue}")
    private String teamAcceptedQueue;

    @Value("${rabbitmq.team.rejected.queue}")
    private String teamRejectedQueue;

    @Value("${rabbitmq.startup.deleted.queue}")
    private String startupDeletedQueue;

    @Value("${rabbitmq.startup.deleted.routing-key}")
    private String startupDeletedRoutingKey;

    @Bean
    public DirectExchange founderLinkExchange() {
        return new DirectExchange(exchange);
    }

    @Bean
    public Queue teamQueue() {
        return new Queue(teamQueue, true);
    }

    @Bean
    public Queue startupDeletedQueue() {
        return new Queue(startupDeletedQueue, true);
    }

    @Bean
    public Queue teamAcceptedQueue() {
        return new Queue(teamAcceptedQueue, true);
    }

    @Bean
    public Queue teamRejectedQueue() {
        return new Queue(teamRejectedQueue, true);
    }

    @Bean
    public Binding teamInviteBinding() {
        return BindingBuilder.bind(teamQueue()).to(founderLinkExchange()).with(teamRoutingKey);
    }

    @Bean
    public Binding teamAcceptedBinding() {
        return BindingBuilder.bind(teamAcceptedQueue()).to(founderLinkExchange()).with(teamAcceptedRoutingKey);
    }

    @Bean
    public Binding teamRejectedBinding() {
        return BindingBuilder.bind(teamRejectedQueue()).to(founderLinkExchange()).with(teamRejectedRoutingKey);
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
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          ObservationRegistry observationRegistry) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        rabbitTemplate.setObservationEnabled(true);
        return rabbitTemplate;
    }
}