package io.github.zxcbecause.pricemonitor.notification;

public interface Notifier {

    /**
     * @param chatId recipient; when null the implementation falls back to its default chat
     */
    void send(Long chatId, String text);
}
