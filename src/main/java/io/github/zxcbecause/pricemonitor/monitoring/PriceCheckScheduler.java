package io.github.zxcbecause.pricemonitor.monitoring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "monitor.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class PriceCheckScheduler {

    private final PriceCheckService service;

    public PriceCheckScheduler(PriceCheckService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${monitor.check-interval:PT15M}", initialDelayString = "PT30S")
    public void run() {
        service.checkAll();
    }
}
