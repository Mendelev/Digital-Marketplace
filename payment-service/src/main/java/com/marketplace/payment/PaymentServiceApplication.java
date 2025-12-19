package com.marketplace.payment;

import com.marketplace.payment.config.AuthServiceProperties;
import com.marketplace.payment.config.JwtProperties;
import com.marketplace.payment.config.PaymentSimulationConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties({
    JwtProperties.class,
    AuthServiceProperties.class,
    PaymentSimulationConfig.class
})
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
