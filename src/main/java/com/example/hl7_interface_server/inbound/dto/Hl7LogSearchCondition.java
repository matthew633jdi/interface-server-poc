package com.example.hl7_interface_server.inbound.dto;

import com.example.hl7_interface_server.inbound.domain.MessageStatus;

import java.time.Instant;

public record Hl7LogSearchCondition(
        String patientId,
        MessageStatus status,
        Instant from,
        Instant to
) {
    public static Hl7LogSearchCondition of(String patientId, MessageStatus status, Instant from, Instant to) {
        return new Hl7LogSearchCondition(patientId, status, from, to);
    }
}
