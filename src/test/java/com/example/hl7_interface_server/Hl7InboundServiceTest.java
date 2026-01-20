package com.example.hl7_interface_server;

import ca.uhn.hl7v2.HL7Exception;
import com.example.hl7_interface_server.inbound.client.EmrApiClient;
import com.example.hl7_interface_server.inbound.domain.Hl7MessageLog;
import com.example.hl7_interface_server.inbound.domain.MessageStatus;
import com.example.hl7_interface_server.inbound.dto.Hl7ObservationDto;
import com.example.hl7_interface_server.inbound.repository.Hl7MessageLogRepository;
import com.example.hl7_interface_server.inbound.service.Hl7InboundService;
import com.example.hl7_interface_server.inbound.service.Hl7ParserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Spring Context 없이 가볍게 테스트
public class Hl7InboundServiceTest {
    @InjectMocks
    private Hl7InboundService inboundService; // 테스트 대상

    @Mock
    private Hl7ParserService parserService;
    @Mock private Hl7MessageLogRepository messageLogRepository;
    @Mock private EmrApiClient emrApiClient;

    @Test
    @DisplayName("정상 흐름: 파싱 -> 저장 -> API전송 -> 상태 SENT 업데이트")
    void processSuccessTest() throws HL7Exception {
        // Given
        String rawMsg = "MSH|...";
        Hl7ObservationDto mockDto = new Hl7ObservationDto("MSG-01", "P123", "Hong", "T01", "Test", "100", "mg", "F", Instant.now());

        // Mocking behavior
        when(parserService.parseOruMessage(rawMsg)).thenReturn(mockDto);
        when(messageLogRepository.findByMessageControlId("MSG-01")).thenReturn(Optional.empty()); // 중복 아님

        // save 호출 시 실제 객체 반환 흉내
        Hl7MessageLog mockLog = Hl7MessageLog.create("MSG-01", rawMsg, "P123", "Hong");
        when(messageLogRepository.save(any(Hl7MessageLog.class))).thenReturn(mockLog);

        // When
        inboundService.processHl7Message(rawMsg);

        // Then
        verify(emrApiClient, times(1)).sendResultToEmr(mockDto); // API 호출 확인
        assertThat(mockLog.getStatus()).isEqualTo(MessageStatus.SENT); // 상태 변경 확인
    }

    @Test
    @DisplayName("멱등성: 이미 SENT 상태인 메시지는 API 호출 없이 종료한다")
    void idempotencySkipTest() throws HL7Exception {
        // Given
        String rawMsg = "MSH|...";
        Hl7ObservationDto mockDto = new Hl7ObservationDto("MSG-EXIST", "P123", "Hong", "T01", "Test", "100", "mg", "F", Instant.now());

        // 이미 SENT 상태인 로그가 DB에 있다고 가정
        Hl7MessageLog existingLog = Hl7MessageLog.create("MSG-EXIST", rawMsg, "P123", "Hong");
        existingLog.markAsSent();

        when(parserService.parseOruMessage(rawMsg)).thenReturn(mockDto);
        when(messageLogRepository.findByMessageControlId("MSG-EXIST")).thenReturn(Optional.of(existingLog));

        // When
        inboundService.processHl7Message(rawMsg);

        // Then
        verify(messageLogRepository, never()).save(any()); // 저장 안 함
        verify(emrApiClient, never()).sendResultToEmr(any()); // 전송 안 함
    }

    @Test
    @DisplayName("장애 발생: API 전송 실패 시 DB 상태를 FAIL로 바꾸고 예외를 던진다")
    void apiFailTest() throws HL7Exception {
        // Given
        String rawMsg = "MSH|...";
        Hl7ObservationDto mockDto = new Hl7ObservationDto("MSG-ERR", "P123", "Hong", "T01", "Test", "100", "mg", "F", Instant.now());
        Hl7MessageLog mockLog = Hl7MessageLog.create("MSG-ERR", rawMsg, "P123", "Hong");

        when(parserService.parseOruMessage(rawMsg)).thenReturn(mockDto);
        when(messageLogRepository.findByMessageControlId("MSG-ERR")).thenReturn(Optional.empty());
        when(messageLogRepository.save(any())).thenReturn(mockLog);

        // API 호출 시 예외 발생 설정
        doThrow(new RuntimeException("API Connection Timeout")).when(emrApiClient).sendResultToEmr(any());

        // When & Then
        // 예외가 던져져야 Spring Retry가 작동하므로, 반드시 예외가 발생하는지 검증
        assertThrows(RuntimeException.class, () -> inboundService.processHl7Message(rawMsg));

        // DB 상태가 FAIL로 변했는지 확인
        assertThat(mockLog.getStatus()).isEqualTo(MessageStatus.FAIL);
        assertThat(mockLog.getErrorMessage()).contains("API Connection Timeout");
    }
}
