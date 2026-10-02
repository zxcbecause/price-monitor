package io.github.zxcbecause.pricemonitor.messaging;

import io.github.zxcbecause.pricemonitor.domain.Money;
import io.github.zxcbecause.pricemonitor.monitoring.AlertPolicy;
import io.github.zxcbecause.pricemonitor.monitoring.PriceChangedEvent;
import io.github.zxcbecause.pricemonitor.notification.Notifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes price change events and sends a Telegram alert when the {@link AlertPolicy} says so.
 */
@Component
public class PriceAlertListener {

    private final AlertPolicy policy;
    private final Notifier notifier;

    public PriceAlertListener(AlertPolicy policy, Notifier notifier) {
        this.policy = policy;
        this.notifier = notifier;
    }

    @KafkaListener(topics = "${monitor.topic:price-changes}", groupId = "price-alerts")
    public void onPriceChanged(PriceChangedEvent event) {
        policy.evaluate(event).ifPresent(reason -> notifier.send(event.chatId(), format(event, reason)));
    }

    static String format(PriceChangedEvent event, AlertPolicy.Reason reason) {
        String title = event.title() != null ? event.title() : "Товар " + event.sku();
        StringBuilder text = new StringBuilder()
                .append("📉 Цена снизилась: ").append(title).append('\n')
                .append(event.marketplace().displayName()).append(" · арт. ").append(event.sku()).append('\n')
                .append(Money.format(event.oldPriceKopecks())).append(" → ")
                .append(Money.format(event.newPriceKopecks()))
                .append(" (−").append(Math.round(event.dropPercent())).append("%)");
        if (reason == AlertPolicy.Reason.TARGET_REACHED) {
            text.append('\n').append("🎯 Цель ").append(Money.format(event.targetPriceKopecks())).append(" достигнута");
        }
        text.append('\n').append(event.marketplace().productUrl(event.sku()));
        return text.toString();
    }
}
