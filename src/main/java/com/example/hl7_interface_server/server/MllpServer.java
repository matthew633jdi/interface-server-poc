package com.example.hl7_interface_server.server;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.app.HL7Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MllpServer {

    private final Hl7MessageApplication hl7MessageApplication;
    private HL7Service service;
    private final int port = 2575;

    @PostConstruct
    public void start() {
        HapiContext context = new DefaultHapiContext();

        service = context.newServer(port, false);

        // 메시지 처리 핸들러 등록
        // "*" (모든 메시지 타입), "*" (모든 트리거 이벤트)에 대해 처리하도록 설정
        service.registerApplication("*", "*", hl7MessageApplication);

        // 서버 시작
        log.info("MLLP HL7Service를 포트 {}에서 시작합니다...", port);
        service.start();
    }

    @PreDestroy
    public void stop() {
        if (service != null) {
            log.info("MLLP HL7Service 종료 중...");
            service.stop();
        }
    }
}
