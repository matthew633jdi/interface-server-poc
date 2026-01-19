package com.example.hl7_interface_server;

import com.example.hl7_interface_server.config.Hl7Properties;
import com.example.hl7_interface_server.inbound.dto.Hl7MessageEnvelope;
import com.example.hl7_interface_server.inbound.service.Hl7ParserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class Hl7InboundIntegrationTest {

    @Autowired private RabbitTemplate rabbitTemplate;

    @Autowired private Hl7Properties properties;

    @MockitoSpyBean private Hl7ParserService parserService;

    @Autowired private RabbitAdmin rabbitAdmin;

    @BeforeEach
    void setUp() {
        rabbitAdmin.purgeQueue(properties.getRabbitmq().getQueue());
        rabbitAdmin.purgeQueue(properties.getRabbitmq().getDlqName());
    }

    @Test
    @DisplayName("정상적인 HL7 메시지는 파싱 후 처리에 성공한다")
    void consumeSuccessTest() throws Exception {
        // Given
        String controlId = "MSG-001";
        String rawMessage = "MSH|^~\\&|SENDING_APP|SENDING_FAC|REC_APP|REC_FAC|202601192340||ORU^R01|" + controlId + "|P|2.3|\r" +
                "PID|1||P12345||KIM^GILDONG||19900101|M\r" +
                "OBR|1|||ORD123|||202601192340\r" +
                "OBX|1|NM|TEST_CODE^Sugar|1|100|mg/dL|70-110|N|||F";
        Hl7MessageEnvelope envelope = new Hl7MessageEnvelope(controlId, rawMessage, Instant.now());

        // When
        rabbitTemplate.convertAndSend(properties.getRabbitmq().getExchange(), properties.getRabbitmq().getRoutingKey(), envelope);

        // Then: 성공 케이스이므로 재시도 없이 딱 1번만 호출되어야 함
        // Awaitility를 사용하면 sleep보다 더 정확하게 대기할 수 있습니다.
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(parserService, times(1)).parseOruMessage(anyString());
        });
    }

    @Test
    @DisplayName("파싱 실패 시 3번 재시도 후 메시지가 DLQ로 이동한다")
    void retryAndDlqTest() throws Exception {
        // Given: 파싱 서비스가 무조건 에러를 던지도록 설정
        doThrow(new RuntimeException("Parsing Error Simulation"))
                .when(parserService).parseOruMessage(anyString());

        String controlId = "ERR-001";
        Hl7MessageEnvelope envelope = new Hl7MessageEnvelope(controlId, "INVALID_MESSAGE", Instant.now());

        // When: 메시지 발행
        rabbitTemplate.convertAndSend(properties.getRabbitmq().getExchange(), properties.getRabbitmq().getRoutingKey(), envelope);

        // Then
        TimeUnit.SECONDS.sleep(5); // 재시도(2s * 3) 시간을 고려하여 대기

        // 1. 재시도가 설정된 횟수(3번)만큼 일어났는지 검증
        verify(parserService, times(3)).parseOruMessage(anyString());

        // 2. DLQ에 메시지가 들어왔는지 확인
        // rabbitTemplate.receiveAndConvert를 사용하여 DLQ에서 메시지를 직접 꺼내 확인
        Object dlqMessage = rabbitTemplate.receiveAndConvert(properties.getRabbitmq().getDlqName());

        assertThat(dlqMessage).isNotNull();
        Hl7MessageEnvelope deadMessage = (Hl7MessageEnvelope) dlqMessage;
        assertThat(deadMessage.messageId()).isEqualTo(controlId);

        System.out.println("DLQ 이동 확인 완료: " + deadMessage.messageId());
    }
}
