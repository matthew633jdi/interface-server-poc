package com.example.hl7_interface_server.inbound.dto;

import java.time.Instant;

public record Hl7ObservationDto(
        String messageControlId,
        String patientId,
        String patientName,
        String testCode,            // OBR-4 or OBX-3
        String testName,
        String observationValue,    // OBX-5
        String unit,                // OBX-6
        String resultStatus,        // OBX-11 (F: Final, P: Preliminary)
        Instant resultTime
) {
}
