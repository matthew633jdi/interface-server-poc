package com.example.hl7_interface_server.inbound.client;

import com.example.hl7_interface_server.inbound.dto.Hl7ObservationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmrApiClient {

    private final RestClient restClient;
    private final RestClient emrRestClient;

    public void sendResultToEmr(Hl7ObservationDto dto) {
        log.info("[HTTP-OUT] call EMR API: PID={}, Result={}", dto.patientId(), dto.observationValue());

        // POST
        emrRestClient.post()
                .uri("/api/v1/emr/results") // Mock 서버 엔드포인트
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)  // DTO를 자동으로 JSON으로 직렬화
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (request, response) -> {
                    String errorBody = new String(response.getBody().readAllBytes());
                    log.error("[HTTP-OUT] EMR 응답 오류: 상태코드={}, 내용={}", response.getStatusCode(), errorBody);
                    // 여기서 예외를 던져야 Transaction Rollback 및 Retry Trigger가 됨
                    throw new RuntimeException("EMR API Error: " + response.getStatusCode());
                })
                .toBodilessEntity(); // 응답 본문이 필요 없다면 이렇게 처리

        log.info("[HTTP-OUT] Successfully sent EMR results to EMR API");
    }
}
