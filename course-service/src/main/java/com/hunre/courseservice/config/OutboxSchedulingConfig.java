package com.hunre.courseservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "course.outbox.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxSchedulingConfig {}
