package io.github.zxcbecause.pricemonitor.notification;

import io.github.zxcbecause.pricemonitor.config.MonitorProperties;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Sends messages through the Telegram Bot API. Without a bot token it only logs,
 * so the app runs locally with zero setup.
 */
@Component
public class TelegramNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotifier.class);

    private final RestClient restClient;
    private final MonitorProperties.Telegram config;

    public TelegramNotifier(RestClient.Builder builder, MonitorProperties properties) {
        this.config = properties.telegram();
        this.restClient = builder.baseUrl(config.baseUrl()).build();
    }

    @Override
    public void send(Long chatId, String text) {
        Long target = chatId != null ? chatId : config.defaultChatId();
        if (!config.enabled() || target == null) {
            log.info("[telegram disabled] {}", text.replace('\n', ' '));
            return;
        }
        restClient.post()
                .uri("/bot{token}/sendMessage", config.botToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("chat_id", target, "text", text, "disable_web_page_preview", true))
                .retrieve()
                .toBodilessEntity();
    }
}
