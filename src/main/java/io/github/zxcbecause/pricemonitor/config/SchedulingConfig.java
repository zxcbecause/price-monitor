package io.github.zxcbecause.pricemonitor.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Scheduling can be switched off (e.g. in tests) with {@code monitor.scheduling.enabled=false}.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "monitor.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
