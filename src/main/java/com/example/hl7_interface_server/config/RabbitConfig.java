package com.example.hl7_interface_server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
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
//        return new Queue(properties.getRabbitmq().getQueue(), true);
        return QueueBuilder.durable(properties.getRabbitmq().getQueue())
                .withArgument("x-dead-letter-exchange", properties.getRabbitmq().getDlxName())
                .withArgument("x-dead-letter-routing-key", properties.getRabbitmq().getDlqRoutingKey())
                .build();
    }

    @Bean
    public Binding binding(Queue hl7Queue, TopicExchange exchange) {
        return BindingBuilder.bind(hl7Queue).to(exchange).with(properties.getRabbitmq().getRoutingKey());
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(properties.getRabbitmq().getDlqName(), true);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(properties.getRabbitmq().getDlxName());
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(properties.getRabbitmq().getDlqRoutingKey());
    }

    /**
     * 실무에서는 RabbitMQ Management UI에서 메시지 내용을 확인하고 다른 언어(Python, Go 등)와의 호환성을 위해 JSON 직렬화가 필수입니다.
     * Hl7MessageEnvelope를 객체 그대로 주고받으려면 아래 빈 설정이 반드시 필요합니다.
     * @return MessageConverter
     */
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        // Instant 등 날짜/시간 모듈 등록
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        return rabbitTemplate;
    }
}
