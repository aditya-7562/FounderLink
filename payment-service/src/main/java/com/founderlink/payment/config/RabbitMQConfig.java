package com.founderlink.payment.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for event-driven saga architecture.
 * 
 * Queue Pattern:
 * investment.created.queue → handles InvestmentCreatedEvent
 * investment.approved.queue → handles InvestmentApprovedEvent
 * investment.rejected.queue → handles InvestmentRejectedEvent
 * 
 * DLQ Pattern:
 * Each main queue has x-dead-letter-exchange = founderlink.dlx
 * Failed messages (max retries exceeded) → DLQ
 * DLQ consumer handles logging, alerts, manual intervention
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange:founderlink.exchange}")
    private String mainExchange;

    @Value("${rabbitmq.dlx:founderlink.dlx}")
    private String dlExchange;

    @Value("${rabbitmq.dlq:founderlink.dlq}")
    private String dlQueue;

    // ============== MAIN QUEUES ==============

    @Bean
    public Queue investmentCreatedQueue() {
        return QueueBuilder.durable("investment.created.queue")
                .withArgument("x-dead-letter-exchange", dlExchange)
                .withArgument("x-dead-letter-routing-key", dlQueue)
                .build();
    }

    @Bean
    public Queue investmentApprovedQueue() {
        return QueueBuilder.durable("investment.approved.queue")
                .withArgument("x-dead-letter-exchange", dlExchange)
                .withArgument("x-dead-letter-routing-key", dlQueue)
                .build();
    }

    @Bean
    public Queue investmentRejectedQueue() {
        return QueueBuilder.durable("investment.rejected.queue")
                .withArgument("x-dead-letter-exchange", dlExchange)
                .withArgument("x-dead-letter-routing-key", dlQueue)
                .build();
    }

    // ============== DEAD LETTER QUEUE ==============

    /**
     * Dead Letter Queue receives messages that failed processing.
     * This queue is for failed events that need manual intervention or retry.
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(dlQueue).build();
    }

    // ============== EXCHANGES ==============

    @Bean
    public DirectExchange founderLinkExchange() {
        return new DirectExchange(mainExchange, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(dlExchange, true, false);
    }

    // ============== BINDINGS ==============

    @Bean
    public Binding investmentCreatedBinding() {
        return BindingBuilder.bind(investmentCreatedQueue())
                .to(founderLinkExchange())
                .with("investment.created");
    }

    @Bean
    public Binding investmentApprovedBinding() {
        return BindingBuilder.bind(investmentApprovedQueue())
                .to(founderLinkExchange())
                .with("investment.approved");
    }

    @Bean
    public Binding investmentRejectedBinding() {
        return BindingBuilder.bind(investmentRejectedQueue())
                .to(founderLinkExchange())
                .with("investment.rejected");
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(dlQueue);
    }

    // ============== MESSAGE CONVERTER ==============

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
