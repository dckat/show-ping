package com.ssginc.showpingrefactoring.batch.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;

@Configuration
public class CustomBackOffPolicy {

    @Bean
    public FixedBackOffPolicy customBackOffPolicy() {
        FixedBackOffPolicy policy = new FixedBackOffPolicy();
        policy.setBackOffPeriod(2000L);
        return policy;
    }

}
