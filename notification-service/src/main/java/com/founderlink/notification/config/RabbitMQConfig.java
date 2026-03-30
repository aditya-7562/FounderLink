package com.founderlink.notification.config;

import org.springframework.amqp.core.*;
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

    @Value("${rabbitmq.queue.startup}")
    private String startupQueue;

    @Value("${rabbitmq.queue.investment}")
    private String investmentQueue;

    @Value("${rabbitmq.queue.team}")
    private String teamQueue;

    @Value("${rabbitmq.queue.messaging}")
    private String messagingQueue;

    @Value("${rabbitmq.queue.team-accepted}")
    private String teamAcceptedQueue;

    @Value("${rabbitmq.queue.team-rejected}")
    private String teamRejectedQueue;

    @Value("${rabbitmq.queue.payment-completed}")
    private String paymentCompletedQueue;

    @Value("${rabbitmq.queue.payment-failed}")
    private String paymentFailedQueue;

    @Value("${rabbitmq.queue.investment-approved}")
    private String investmentApprovedQueue;

    @Value("${rabbitmq.queue.investment-rejected}")
    private String investmentRejectedQueue;

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
    public Queue passwordResetQueue() {
        return new Queue("password-reset-queue", true);
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
    public Queue paymentCompletedQueue() {
        return new Queue(paymentCompletedQueue, true);
    }

    @Bean
    public Queue paymentFailedQueue() {
        return new Queue(paymentFailedQueue, true);
    }

    @Bean
    public Queue investmentApprovedQueue() {
        return new Queue(investmentApprovedQueue, true);
    }

    @Bean
    public Queue investmentRejectedQueue() {
        return new Queue(investmentRejectedQueue, true);
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
    public Binding passwordResetBinding(Queue passwordResetQueue, DirectExchange exchange) {
        return BindingBuilder.bind(passwordResetQueue()).to(exchange).with("password.reset");
    }

    @Bean
    public Binding teamAcceptedBinding(Queue teamAcceptedQueue, DirectExchange exchange) {
        return BindingBuilder.bind(teamAcceptedQueue()).to(exchange).with("team.member.accepted");
    }

    @Bean
    public Binding teamRejectedBinding(Queue teamRejectedQueue, DirectExchange exchange) {
        return BindingBuilder.bind(teamRejectedQueue()).to(exchange).with("team.member.rejected");
    }

    @Bean
    public Binding paymentCompletedBinding(Queue paymentCompletedQueue, DirectExchange exchange) {
        return BindingBuilder.bind(paymentCompletedQueue()).to(exchange).with("payment.completed");
    }

    @Bean
    public Binding paymentFailedBinding(Queue paymentFailedQueue, DirectExchange exchange) {
        return BindingBuilder.bind(paymentFailedQueue()).to(exchange).with("payment.failed");
    }

    @Bean
    public Binding investmentApprovedBinding(Queue investmentApprovedQueue, DirectExchange exchange) {
        return BindingBuilder.bind(investmentApprovedQueue()).to(exchange).with("investment.approved");
    }

    @Bean
    public Binding investmentRejectedBinding(Queue investmentRejectedQueue, DirectExchange exchange) {
        return BindingBuilder.bind(investmentRejectedQueue()).to(exchange).with("investment.rejected");
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ── Trace propagation: extracts TraceId from incoming RabbitMQ messages ──
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          ObservationRegistry observationRegistry) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        template.setObservationEnabled(true);
        return template;
    }
}
