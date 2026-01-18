package com.example.hl7_interface_server.server;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.protocol.ReceivingApplication;
import ca.uhn.hl7v2.protocol.ReceivingApplicationException;
import ca.uhn.hl7v2.util.Terser;
import com.example.hl7_interface_server.config.Hl7Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class Hl7MessageApplication implements ReceivingApplication<Message> {

    private final RabbitTemplate rabbitTemplate;
    private final Hl7Properties properties;
    private final HapiContext hapiContext;

    @Override
    public Message processMessage(Message message, Map<String, Object> metadata) throws ReceivingApplicationException, HL7Exception {

        // Terser를 사용하여 원본 메시지의 MSH-10 추출
        // 내부적으로 파싱 비용이 발생하므로 성능이 민감한 대용량 환경에서는 최적화가 필수
        Terser terser = new Terser(message);
        // RabbitMQ로 메시지 발행 (비동기 처리의 시작)
        // message 고유 식별자 추출 (로그 추적용)
        String messageControlId = "UNKNOWN";
        try {
            messageControlId = terser.get("/MSH-10");
        } catch (HL7Exception e) {
            log.warn("MSH-10 필드를 추출할 수 없습니다. 기본값을 사용합니다.");
        }

        // MDC를 활용한 로그 컨텍스트 설정 (모든 로그에 ID 포함)
        MDC.put("messageId", messageControlId);

        try {
            log.info("HL7 message 수신 (Type: {}, Event: {})", terser.get("/MSH-9-1"), terser.get("/MSH-9-2"));

            // 전송 확인을 위한 CorrelationData 생성
            CorrelationData correlationData = new CorrelationData(messageControlId);


            // queue.enqueue 역할, 내부적으로 TCP 연결을 맺고 RabbitMQ 서버의 Queue의 이름에 데이터를 넣음
            rabbitTemplate.convertAndSend(
                    properties.getRabbitmq().getExchange(),
                    properties.getRabbitmq().getRoutingKey(),
                    message.toString(),
                    correlationData
            );

            log.info("RabbitMQ 발행 요청 완료");
            return message.generateACK();
        } catch (AmqpException e) {
            log.error("message broker 전송 중 인프라 오류: {}", e.getMessage());
            // HAPI는 이 예외를 받으면 자동으로 AE(Application Error) 응답을 생성한다.
            throw new ReceivingApplicationException("Broker Connection Error", e);
        } catch (Exception e) {
            log.error("message 처리 중 예외 발생: ", e);
            throw new HL7Exception(e);
        } finally {
            // 로그 컨텍스트 초기화
            MDC.clear();
        }

    }

    @Override
    public boolean canProcess(Message message) {
        return true;
    }
}
