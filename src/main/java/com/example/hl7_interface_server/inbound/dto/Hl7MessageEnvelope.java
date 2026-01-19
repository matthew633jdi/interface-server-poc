package com.example.hl7_interface_server.inbound.dto;

import java.io.Serializable;
import java.time.Instant;
/**
 * 큐를 통해 전달되는 메시지 엔벨로프
 * 비즈니스 데이터(rawMessage)와 메타데이터(ID, 수신시각)를 묶는 역할을 합니다.
 */
public record Hl7MessageEnvelope(
        String messageId,        // MSH-10 (Correlation ID)
        String rawMessage,       // 원문 (재시도나 수동 복구 시 필수)
        Instant receivedAt       // 수신 시각 (UTC 기준 Instant 사용)
) implements Serializable {
}
