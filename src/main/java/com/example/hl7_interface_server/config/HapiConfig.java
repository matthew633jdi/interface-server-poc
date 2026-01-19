package com.example.hl7_interface_server.config;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.parser.ParserConfiguration;
import ca.uhn.hl7v2.validation.impl.NoValidation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HapiConfig {

    @Bean(destroyMethod = "close")
    public HapiContext hapiContext() {
        HapiContext context = new DefaultHapiContext();

        // Parser 설정 최적화
        ParserConfiguration config = context.getParserConfiguration();

        // 비표준 segment(Z-segment) 허용 여부 등 설정 가능
        config.setAllowUnknownVersions(true);

        // 유효성 검사 설정 (운영 환경에 따라 NoValidation or DefaultValidation)
        // 초기 단계에서는 엄격한 검사로 인한 중단을 막기 위해 NoValidation 자주 사용
        context.setValidationContext(new NoValidation());

        return context;
    }
}
