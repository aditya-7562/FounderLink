package com.founderlink.Startup_Service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

        public static final String EXCHANGE = "startup-exchange";
        public static final String QUEUE = "startup-queue";
        public static final String ROUTING_KEY = "startup.created";

        // Queue
        @Bean
        public Queue queue() {
            return new Queue(QUEUE);
        }

        // Exchange
        @Bean
        public TopicExchange exchange() {
            return new TopicExchange(EXCHANGE);
        }

        // Binding
        @Bean
        public Binding binding(Queue queue, TopicExchange exchange) {
            return BindingBuilder
                    .bind(queue)
                    .to(exchange)
                    .with(ROUTING_KEY);
        }

        // JSON Converter
        @Bean
        public MessageConverter converter() {
            return new Jackson2JsonMessageConverter();
        }
}
