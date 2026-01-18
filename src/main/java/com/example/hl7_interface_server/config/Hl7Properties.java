package com.example.hl7_interface_server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter @Setter
@Configuration
@ConfigurationProperties(prefix = "hl7")
public class Hl7Properties {

    private Server server = new Server();
    private Rabbitmq rabbitmq = new Rabbitmq();

    @Getter @Setter
    public static class Server {
        private int port;
    }

    @Getter @Setter
    public static class Rabbitmq {
        private String exchange;
        private String queue;
        private String routingKey;
    }
}
