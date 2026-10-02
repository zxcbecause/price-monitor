package io.github.zxcbecause.pricemonitor.messaging;

import io.github.zxcbecause.pricemonitor.config.MonitorProperties;
import io.github.zxcbecause.pricemonitor.monitoring.PriceChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PriceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PriceEventPublisher.class);

    private final KafkaTemplate<String, PriceChangedEvent> kafka;
    private final String topic;

    public PriceEventPublisher(KafkaTemplate<String, PriceChangedEvent> kafka, MonitorProperties properties) {
        this.kafka = kafka;
        this.topic = properties.topic();
    }

    /** Key = product id, so all events of one product land in the same partition and keep their order. */
    public void publish(PriceChangedEvent event) {
        kafka.send(topic, String.valueOf(event.productId()), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish price change for product {}", event.productId(), error);
                    } else {
                        log.debug("Published price change for product {} to {}", event.productId(),
                                result.getRecordMetadata());
                    }
                });
    }
}
