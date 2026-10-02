package io.github.zxcbecause.pricemonitor.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * All application-specific settings live under the {@code monitor.*} prefix.
 */
@ConfigurationProperties("monitor")
public record MonitorProperties(
        @DefaultValue("PT15M") Duration checkInterval,
        @DefaultValue("10") int defaultDropThresholdPercent,
        @DefaultValue("price-changes") String topic,
        @DefaultValue Telegram telegram,
        @DefaultValue Wildberries wildberries,
        @DefaultValue Ozon ozon
) {

    public record Telegram(
            @DefaultValue("") String botToken,
            Long defaultChatId,
            @DefaultValue("https://api.telegram.org") String baseUrl
    ) {
        public boolean enabled() {
            return botToken != null && !botToken.isBlank();
        }
    }

    public record Wildberries(
            @DefaultValue("https://card.wb.ru") String baseUrl,
            @DefaultValue("-1257786") String dest
    ) {
    }

    public record Ozon(
            @DefaultValue("https://api-seller.ozon.ru") String baseUrl,
            @DefaultValue("") String clientId,
            @DefaultValue("") String apiKey
    ) {
        public boolean configured() {
            return !clientId.isBlank() && !apiKey.isBlank();
        }
    }
}
