package com.example.hl7_interface_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient emrRestClient(RestClient.Builder builder) {
        // 실무 팁: 타임아웃은 필수입니다. (연결 3초, 읽기 5초)
        // EMR 서버가 5초 안에 응답 안 하면 에러로 간주하고 우리 쪽에서 끊고 재시도(Retry) 로직을 태우기 위함입니다.
        return builder
                .baseUrl("http://localhost:8080") // 테스트용 로컬 주소 (실제 운영에선 application.yml에서 주입)
                .requestFactory(new HttpComponentsClientHttpRequestFactory()) // Apache HttpClient 사용 권장
                .build();
    }
}
