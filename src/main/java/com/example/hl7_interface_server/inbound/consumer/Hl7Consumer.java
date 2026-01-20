package com.example.hl7_interface_server.inbound.consumer;

import ca.uhn.hl7v2.HL7Exception;
import com.example.hl7_interface_server.inbound.dto.Hl7MessageEnvelope;
import com.example.hl7_interface_server.inbound.service.Hl7InboundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Hl7Consumer {

    private final Hl7InboundService inboundService;

    @RabbitListener(queues = "${hl7.rabbitmq.queue}")
    public void consumeHl7Message(Hl7MessageEnvelope envelope) throws HL7Exception {
        // [실무 포인트] Consumer 스레드에서도 MDC를 설정하여 Producer-Consumer 간 로그 추적 연결
        MDC.put("messageId", envelope.messageId());

        try {
            log.info("메시지 수신 - 수신시각: {}", envelope.receivedAt());

            inboundService.processHl7Message(envelope.rawMessage());

            log.info("처리 성공: {}", envelope.messageId());
        } catch (Exception e) {
            log.error("메시지 처리 실패: {}. (Spring Retry가 재시도를 수행합니다)", e.getMessage());

            // 핵심: 여기서 예외를 그대로 던지면 application.yml 설정에 따라 재시도함.
            // 모든 재시도 소진 시 RabbitConfig에 설정된 x-dead-letter-exchange로 자동 이동.
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
