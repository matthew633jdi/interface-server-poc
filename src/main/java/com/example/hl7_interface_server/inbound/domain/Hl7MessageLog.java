package com.example.hl7_interface_server.inbound.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hl7MessageLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String messageControlId;

    @Lob
    @Column(nullable = false)
    private String rawMessage;

    private String patientId;
    private String patientName;

    @Enumerated(EnumType.STRING)
    private MessageStatus status;

    private int retryCount = 0;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Instant receivedAt;
    private Instant lastModifiedAt;

    private Hl7MessageLog(String messageControlId, String rawMessage, String patientId, String patientName) {
        this.messageControlId = messageControlId;
        this.rawMessage = rawMessage;
        this.patientId = patientId;
        this.patientName = patientName;
        this.status = MessageStatus.RECEIVED;
        this.receivedAt = Instant.now();
        this.lastModifiedAt = Instant.now();
    }

    public static Hl7MessageLog create(String messageControlId, String rawMessage, String patientId, String patientName) {
        return new Hl7MessageLog(messageControlId, rawMessage, patientId, patientName);
    }

    public void markAsSent() {
        this.status = MessageStatus.SENT;
        this.lastModifiedAt = Instant.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = MessageStatus.FAIL;
        this.errorMessage = errorMessage;
        this.retryCount++;
        this.lastModifiedAt = Instant.now();
    }
}
