package com.varun.payment_client;


import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentConfigController {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfigController.class);
    @Value("${payment.gateway-url}")
    private String gatewayUrl;

    @Value("${payment.request-timeout}")
    private int requestTimeout;

    @Value("${payment.currency}")
    private String currency;

    @Value("${logging.level.root}")
    private String loggingLevel;

    @GetMapping("/payment/config")
    public Map<String, Object> configuration() {

        return Map.of(
                "gatewayUrl", gatewayUrl,
                "requestTimeout", requestTimeout,
                "currency", currency,
                "loggingLevel",loggingLevel
        );
    }
}