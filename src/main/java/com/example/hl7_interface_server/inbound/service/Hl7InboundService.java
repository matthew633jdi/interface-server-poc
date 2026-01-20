package com.example.hl7_interface_server.inbound.service;

import ca.uhn.hl7v2.HL7Exception;
import com.example.hl7_interface_server.inbound.client.EmrApiClient;
import com.example.hl7_interface_server.inbound.domain.Hl7MessageLog;
import com.example.hl7_interface_server.inbound.domain.MessageStatus;
import com.example.hl7_interface_server.inbound.dto.Hl7ObservationDto;
import com.example.hl7_interface_server.inbound.repository.Hl7MessageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class Hl7InboundService {

    private final Hl7ParserService parserService;
    private final Hl7MessageLogRepository messageLogRepository;
    private final EmrApiClient emrApiClient;

    @Transactional
    public void processHl7Message(String rawMessage) throws HL7Exception {
        // parsing
        Hl7ObservationDto dto = parserService.parseOruMessage(rawMessage);

        // 멱등성(Idempotency) 체크: 이미 저장된 메시지인지 확인
        // MSH-10(MessageControlID)은 유니크해야 합니다.
        Optional<Hl7MessageLog> existingLog = messageLogRepository.findByMessageControlId(dto.messageControlId());

        if (existingLog.isPresent()) {
            Hl7MessageLog hl7Log = existingLog.get();
            // 이미 성공한 메서드면 중복 처리 방지(로그만 남기고 종료)
            if (hl7Log.getStatus() == MessageStatus.SENT) {
                log.info("이미 처리 완료된 메시지입니다. Skip: {}", dto.messageControlId());
                return;
            }
            // 실패했던 메시지라면 재시도 로직을 타게 됨
        }

        Hl7MessageLog messageLog = existingLog.orElseGet(() ->
                messageLogRepository.save(Hl7MessageLog.create(
                        dto.messageControlId(),
                        rawMessage,
                        dto.patientId(),
                        dto.patientName()
                ))
        );

        try {
            // 4. EMR API 전송 (Mock)
            emrApiClient.sendResultToEmr(dto);

            // 5. 성공 시 상태 업데이트 (Success Update)
            messageLog.markAsSent();

        } catch (Exception e) {
            // 6. 실패 시 상태 업데이트 (Fail Update)
            log.error("EMR 전송 실패: {}", e.getMessage());
            messageLog.markAsFailed(e.getMessage());

            // [중요] 여기서 예외를 다시 던져야 Spring Retry가 작동합니다!
            // DB에는 FAIL로 남기고, 인프라 레벨에서는 재시도를 트리거합니다.
            throw e;
        }
    }
}
