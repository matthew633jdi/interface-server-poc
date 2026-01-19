package com.example.hl7_interface_server.inbound.service;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Terser;
import com.example.hl7_interface_server.inbound.dto.Hl7ObservationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class Hl7ParserService {

    private final HapiContext hapiContext;

    public Hl7ObservationDto parseOruMessage(String rawMessage) throws HL7Exception {
        // String -> Message 객체 변환
        Message message = hapiContext.getPipeParser().parse(rawMessage);
        Terser terser = new Terser(message);
        // 2. 데이터 추출 (ORU^R01 메시지 구조 기준 예시)
        try {
            return new Hl7ObservationDto(
                    terser.get("/MSH-10"),
                    terser.get("/PATIENT_RESULT/PATIENT/PID-3-1"),
                    terser.get("/PATIENT_RESULT/PATIENT/PID-5-1"),
                    terser.get("/PATIENT_RESULT/ORDER_OBSERVATION/OBR-4-1"),
                    terser.get("/PATIENT_RESULT/ORDER_OBSERVATION/OBR-4-2"),
                    terser.get("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION(0)/OBX-5-1"),
                    terser.get("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION(0)/OBX-6-1"),
                    terser.get("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION(0)/OBX-11"),
                    parseHl7DateToInstant(terser.get("/MSH-7"))
            );
        } catch (HL7Exception e) {
            log.error("HL7 필드 추출 중 오류 발생: {}", e.getMessage());
            throw e;
        }
    }

    private Instant parseHl7DateToInstant(String hl7Date) {
        if (hl7Date == null || hl7Date.length() < 8) return Instant.now();
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            // 타임존을 고려하여 시스템 기본 혹은 UTC로 변환 후 Instant 추출
            return LocalDateTime.parse(hl7Date.substring(0, 14), formatter)
                    .atZone(ZoneId.systemDefault())
                    .toInstant();
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}, 현재 시간으로 대체합니다.", hl7Date);
            return Instant.now();
        }
    }
}
