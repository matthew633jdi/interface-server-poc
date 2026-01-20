package com.example.hl7_interface_server.inbound.repository;

import com.example.hl7_interface_server.inbound.domain.Hl7MessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Hl7MessageLogRepository extends JpaRepository<Hl7MessageLog, Long>, Hl7MessageLogRepositoryCustom {

    Optional<Hl7MessageLog> findByMessageControlId(String messageControlId);
}
