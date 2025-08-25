package org.example.gatewayvn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.web.client.RestTemplate;

@EnableJms
@SpringBootApplication
public class GatewayVnApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayVnApplication.class, args);
    }
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
