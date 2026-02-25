package com.ssginc.showpingrefactoring.batch.util;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.policy.SimpleRetryPolicy;

@Configuration
public class CustomRetryTemplate {

    @Bean(name = "CustomRetryTemplate")
    RetryTemplate customRetryTemplate(@Qualifier("CustomBackOffPolicy") BackOffPolicy backOffPolicy) {
        RetryTemplate retryTemplate = new RetryTemplate();

        var retryPolicy = new SimpleRetryPolicy(
                3, // max attempts
                java.util.Map.of(
                        java.net.SocketTimeoutException.class, true,
                        org.springframework.web.client.HttpServerErrorException.class, true,
                        org.springframework.dao.TransientDataAccessException.class, true
                ),
                true
        );

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }
}
