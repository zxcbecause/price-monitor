package io.github.zxcbecause.pricemonitor.api;

import io.github.zxcbecause.pricemonitor.domain.Marketplace;
import io.github.zxcbecause.pricemonitor.domain.Money;
import io.github.zxcbecause.pricemonitor.domain.PriceSnapshot;
import io.github.zxcbecause.pricemonitor.domain.TrackedProduct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record CreateProductRequest(
            @NotNull Marketplace marketplace,
            @NotBlank @Pattern(regexp = "\\d{1,20}", message = "must be a numeric article") String sku,
            @Positive BigDecimal targetPrice,
            @Min(1) @Max(99) Integer dropThresholdPercent,
            Long chatId
    ) {
    }

    public record UpdateProductRequest(
            @Positive BigDecimal targetPrice,
            @Min(1) @Max(99) Integer dropThresholdPercent,
            Boolean active
    ) {
    }

    public record ProductResponse(
            Long id,
            Marketplace marketplace,
            String sku,
            String title,
            String url,
            BigDecimal currentPrice,
            BigDecimal targetPrice,
            Integer dropThresholdPercent,
            boolean active,
            Instant lastCheckedAt
    ) {
        static ProductResponse from(TrackedProduct p) {
            return new ProductResponse(
                    p.getId(), p.getMarketplace(), p.getSku(), p.getTitle(),
                    p.getMarketplace().productUrl(p.getSku()),
                    Money.toRublesOrNull(p.getLastPriceKopecks()),
                    Money.toRublesOrNull(p.getTargetPriceKopecks()),
                    p.getDropThresholdPercent(), p.isActive(), p.getLastCheckedAt());
        }
    }

    public record PricePoint(Instant at, BigDecimal price) {
        static PricePoint from(PriceSnapshot s) {
            return new PricePoint(s.getCapturedAt(), Money.toRubles(s.getPriceKopecks()));
        }
    }

    public record PriceStats(BigDecimal min, BigDecimal max, BigDecimal current, int points) {
    }
}
