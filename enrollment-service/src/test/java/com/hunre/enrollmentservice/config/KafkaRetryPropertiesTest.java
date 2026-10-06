package com.hunre.enrollmentservice.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaRetryPropertiesTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner().withUserConfiguration(RetryConfig.class);

    @Test
    void defaultsAllowFortySecondOutageButEventuallyExhaustRetryBudget() {
        context.run(app -> {
            assertThat(app).hasNotFailed();
            var retry = app.getBean(KafkaRetryProperties.class);
            var backOff = new ExponentialBackOff(retry.initialInterval().toMillis(), retry.multiplier());
            backOff.setMaxInterval(retry.maxInterval().toMillis());
            backOff.setMaxElapsedTime(retry.maxElapsedTime().toMillis());
            var execution = backOff.start();
            long total = 0;
            long delay;
            int attempts = 0;
            while ((delay = execution.nextBackOff()) != BackOffExecution.STOP && attempts < 100) {
                assertThat(delay).isBetween(1000L, 30000L);
                total += delay;
                attempts++;
            }
            assertThat(attempts).isLessThan(100);
            assertThat(total).isBetween(300000L, 330000L).isGreaterThan(40000L);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"initial-interval=0ms", "multiplier=0.5", "max-interval=10ms", "max-elapsed-time=0ms"})
    void invalidRetrySettingsFailAtStartup(String setting) {
        context.withPropertyValues("elearning.kafka.retry." + setting).run(app -> assertThat(app).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(KafkaRetryProperties.class)
    static class RetryConfig { }
}
