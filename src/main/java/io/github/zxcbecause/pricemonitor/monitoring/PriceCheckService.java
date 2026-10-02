package io.github.zxcbecause.pricemonitor.monitoring;

import io.github.zxcbecause.pricemonitor.domain.PriceSnapshot;
import io.github.zxcbecause.pricemonitor.domain.PriceSnapshotRepository;
import io.github.zxcbecause.pricemonitor.domain.TrackedProduct;
import io.github.zxcbecause.pricemonitor.domain.TrackedProductRepository;
import io.github.zxcbecause.pricemonitor.marketplace.MarketplaceClientRegistry;
import io.github.zxcbecause.pricemonitor.marketplace.PriceFetchException;
import io.github.zxcbecause.pricemonitor.marketplace.ProductPrice;
import io.github.zxcbecause.pricemonitor.messaging.PriceEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PriceCheckService {

    private static final Logger log = LoggerFactory.getLogger(PriceCheckService.class);

    private final TrackedProductRepository products;
    private final PriceSnapshotRepository snapshots;
    private final MarketplaceClientRegistry clients;
    private final PriceEventPublisher publisher;
    private final TransactionTemplate tx;
    private final Clock clock;

    public PriceCheckService(TrackedProductRepository products,
                             PriceSnapshotRepository snapshots,
                             MarketplaceClientRegistry clients,
                             PriceEventPublisher publisher,
                             TransactionTemplate tx) {
        this.products = products;
        this.snapshots = snapshots;
        this.clients = clients;
        this.publisher = publisher;
        this.tx = tx;
        this.clock = Clock.systemUTC();
    }

    /** Checks every active product. One failing product never stops the others. */
    public CheckSummary checkAll() {
        int checked = 0;
        int changed = 0;
        int failed = 0;
        for (TrackedProduct product : products.findAllByActiveTrue()) {
            try {
                if (check(product.getId()).isPresent()) {
                    changed++;
                }
                checked++;
            } catch (PriceFetchException e) {
                failed++;
                log.warn("Price check failed for {} {}: {}", product.getMarketplace(), product.getSku(), e.getMessage());
            }
        }
        log.info("Price check finished: checked={}, changed={}, failed={}", checked, changed, failed);
        return new CheckSummary(checked, changed, failed);
    }

    /**
     * Fetches the current price, stores a snapshot and publishes an event if the price changed.
     * The network call happens outside the DB transaction.
     */
    public Optional<PriceChangedEvent> check(Long productId) {
        TrackedProduct product = products.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown product " + productId));
        ProductPrice price = clients.get(product.getMarketplace()).fetchPrice(product.getSku());
        Instant now = clock.instant();

        Optional<PriceChangedEvent> event = tx.execute(status -> {
            TrackedProduct fresh = products.findById(productId).orElseThrow();
            Long previous = fresh.observe(price.title(), price.priceKopecks(), now);
            snapshots.save(new PriceSnapshot(fresh.getId(), price.priceKopecks(), now));
            products.save(fresh);
            if (previous == null || previous == price.priceKopecks()) {
                return Optional.<PriceChangedEvent>empty();
            }
            return Optional.of(new PriceChangedEvent(
                    fresh.getId(), fresh.getMarketplace(), fresh.getSku(), fresh.getTitle(),
                    previous, price.priceKopecks(), fresh.getTargetPriceKopecks(),
                    fresh.getDropThresholdPercent(), fresh.getChatId(), now));
        });

        event.ifPresent(publisher::publish);
        return event;
    }

    public record CheckSummary(int checked, int changed, int failed) {
    }
}
