package com.example.hl7_interface_server.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitConfig {

    private final Hl7Properties properties;

    @Bean
    public TopicExchange hl7Exchange() {
        return new TopicExchange(properties.getRabbitmq().getExchange());
    }

    @Bean
    public Queue hl7Queue() {
        // Durable: true 설정으로 서버 재시작 시에도 메시지 보존
        return new Queue(properties.getRabbitmq().getQueue(), true);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(properties.getRabbitmq().getRoutingKey());
    }
}
