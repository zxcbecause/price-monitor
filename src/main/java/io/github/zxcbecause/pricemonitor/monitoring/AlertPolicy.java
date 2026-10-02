package io.github.zxcbecause.pricemonitor.monitoring;

import io.github.zxcbecause.pricemonitor.config.MonitorProperties;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Decides whether a price change is worth a notification. Pure logic, no I/O.
 */
@Component
public class AlertPolicy {

    public enum Reason {
        /** Price is at or below the target set by the user. */
        TARGET_REACHED,
        /** Price dropped by at least the configured percentage in one step. */
        BIG_DROP
    }

    private final int defaultDropThresholdPercent;

    @Autowired
    public AlertPolicy(MonitorProperties properties) {
        this(properties.defaultDropThresholdPercent());
    }

    AlertPolicy(int defaultDropThresholdPercent) {
        this.defaultDropThresholdPercent = defaultDropThresholdPercent;
    }

    public Optional<Reason> evaluate(PriceChangedEvent event) {
        if (!event.isDrop()) {
            return Optional.empty();
        }
        Long target = event.targetPriceKopecks();
        if (target != null && event.newPriceKopecks() <= target) {
            return Optional.of(Reason.TARGET_REACHED);
        }
        int threshold = event.dropThresholdPercent() != null
                ? event.dropThresholdPercent()
                : defaultDropThresholdPercent;
        if (event.dropPercent() >= threshold) {
            return Optional.of(Reason.BIG_DROP);
        }
        return Optional.empty();
    }
}
