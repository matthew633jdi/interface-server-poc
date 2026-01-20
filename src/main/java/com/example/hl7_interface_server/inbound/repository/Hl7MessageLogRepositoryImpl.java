package com.example.hl7_interface_server.inbound.repository;

import com.example.hl7_interface_server.inbound.domain.Hl7MessageLog;
import com.example.hl7_interface_server.inbound.domain.MessageStatus;
import com.example.hl7_interface_server.inbound.dto.Hl7LogSearchCondition;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static com.example.hl7_interface_server.inbound.domain.QHl7MessageLog.hl7MessageLog;

@RequiredArgsConstructor
public class Hl7MessageLogRepositoryImpl implements Hl7MessageLogRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<Hl7MessageLog> searchLogs(Hl7LogSearchCondition condition) {
        return jpaQueryFactory
                .selectFrom(hl7MessageLog)
                .where(
                    patientEq(condition.patientId()),
                        statusEq(condition.status()),
                        receivedAtBetween(condition.from(), condition.to())
                ).fetch();
    }

    private BooleanExpression patientEq(String patientId) {
        return StringUtils.hasText(patientId) ? hl7MessageLog.patientId.eq(patientId) : null;
    }

    private BooleanExpression statusEq(MessageStatus status) {
        return Objects.nonNull(status) ? hl7MessageLog.status.eq(status) : null;
    }

    private BooleanExpression receivedAtBetween(Instant from, Instant to) {
        return Objects.nonNull(from) && Objects.nonNull(to) ? hl7MessageLog.receivedAt.between(from, to) : null;
    }
}
