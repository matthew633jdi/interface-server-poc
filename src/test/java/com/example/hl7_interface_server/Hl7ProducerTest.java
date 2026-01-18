package com.example.hl7_interface_server;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.util.Terser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class Hl7ProducerTest {

    @Test
    @DisplayName("HL7 메시지 전송 시 서버로부터 AA응답을 받음")
    void receiveSuccessACK() throws Exception {
        // Given: 테스트용 메시지 생성 및 제어 번호 설정
        HapiContext context = new DefaultHapiContext();

        ADT_A01 adt = new ADT_A01();
        adt.initQuickstart("ADT", "A01", "P");
        String controlId = "CONT-12345";
        adt.getMSH().getMessageControlID().setValue(controlId);

        // When
        Connection connection = context.newClient("localhost", 2575, false);
        Message response = connection.getInitiator().sendAndReceive(adt);

        // Then
        Terser terser = new Terser(response);
        assertThat(response)
                .as("응답 메시지는 null일 수 없습니다.")
                .isNotNull();

        String ackCode = terser.get("/MSA-1");
        assertThat(ackCode)
                .as("정상 수신 응답인 'AA' 코드를 받아야 합니다.")
                .isEqualTo("AA");

        String responseControlId = terser.get("/MSA-2");
        assertThat(responseControlId)
                .as("송신한 메시지의 제어 번호와 응답의 번호가 일치해야 합니다.")
                .isEqualTo(controlId);

        connection.close();
    }
}
