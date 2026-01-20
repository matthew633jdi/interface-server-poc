package com.example.hl7_interface_server.inbound.repository;

import com.example.hl7_interface_server.inbound.domain.Hl7MessageLog;
import com.example.hl7_interface_server.inbound.dto.Hl7LogSearchCondition;

import java.util.List;

public interface Hl7MessageLogRepositoryCustom {
    List<Hl7MessageLog> searchLogs(Hl7LogSearchCondition condition);
}
