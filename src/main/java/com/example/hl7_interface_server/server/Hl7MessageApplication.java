package com.example.hl7_interface_server.server;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.protocol.ReceivingApplication;
import ca.uhn.hl7v2.protocol.ReceivingApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class Hl7MessageApplication implements ReceivingApplication<Message> {

    @Override
    public Message processMessage(Message message, Map<String, Object> map) throws ReceivingApplicationException, HL7Exception {
        // 수신된 HL7 메시지 원문 출력
        String encodedMessage = message.toString();
        log.info("Received HL7 message: {}", encodedMessage);

        try {
            // 장비에 보낼 응답(ACK) 메시지 자동 생성
            return message.generateACK();
        } catch (IOException e) {
            throw new HL7Exception("Could not generate ACK", e);
        }
    }

    @Override
    public boolean canProcess(Message message) {
        return true;
    }
}
