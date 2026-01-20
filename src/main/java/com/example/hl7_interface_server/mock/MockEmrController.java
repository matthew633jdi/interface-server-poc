package com.example.hl7_interface_server.mock;

import com.example.hl7_interface_server.inbound.dto.Hl7ObservationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/emr")
public class MockEmrController {

    @PostMapping("/results")
    public ResponseEntity<String> receiveResult(@RequestBody Hl7ObservationDto dto) {
        log.info("[Mock EMR Server] 데이터 수신 확인: {}", dto);

        // [테스트 시나리오]
        // 특정 환자 ID(예: "ERROR_PATIENT")가 오면 500 에러를 낸다.
        // -> 그러면 Interface Server가 3번 재시도 후 DLQ로 보내는지 확인할 수 있음!
        if ("ERROR_PATIENT".equals(dto.patientId())) {
            log.warn("[Mock EMR Server] 장애 상황 시뮬레이션 (500 Error)");
            return ResponseEntity.internalServerError().body("EMR DB Connection Failed (Simulation)");
        }

        return ResponseEntity.ok("Received Successfully");
    }
}
