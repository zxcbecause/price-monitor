package io.github.zxcbecause.pricemonitor.api;

import io.github.zxcbecause.pricemonitor.api.ProductDtos.CreateProductRequest;
import io.github.zxcbecause.pricemonitor.api.ProductDtos.PricePoint;
import io.github.zxcbecause.pricemonitor.api.ProductDtos.PriceStats;
import io.github.zxcbecause.pricemonitor.api.ProductDtos.ProductResponse;
import io.github.zxcbecause.pricemonitor.api.ProductDtos.UpdateProductRequest;
import io.github.zxcbecause.pricemonitor.domain.Money;
import io.github.zxcbecause.pricemonitor.domain.PriceSnapshot;
import io.github.zxcbecause.pricemonitor.domain.PriceSnapshotRepository;
import io.github.zxcbecause.pricemonitor.domain.TrackedProduct;
import io.github.zxcbecause.pricemonitor.domain.TrackedProductRepository;
import io.github.zxcbecause.pricemonitor.monitoring.PriceCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.LongSummaryStatistics;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Tracked marketplace products")
public class ProductController {

    private final TrackedProductRepository products;
    private final PriceSnapshotRepository snapshots;
    private final PriceCheckService checker;

    public ProductController(TrackedProductRepository products,
                             PriceSnapshotRepository snapshots,
                             PriceCheckService checker) {
        this.products = products;
        this.snapshots = snapshots;
        this.checker = checker;
    }

    @PostMapping
    @Operation(summary = "Start tracking a product")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        if (products.existsByMarketplaceAndSku(request.marketplace(), request.sku())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Product %s is already tracked on %s".formatted(request.sku(), request.marketplace()));
        }
        TrackedProduct saved = products.save(new TrackedProduct(
                request.marketplace(), request.sku(),
                Money.toKopecksOrNull(request.targetPrice()),
                request.dropThresholdPercent(), request.chatId()));
        return ResponseEntity.created(URI.create("/api/products/" + saved.getId()))
                .body(ProductResponse.from(saved));
    }

    @GetMapping
    @Operation(summary = "List tracked products")
    public List<ProductResponse> list() {
        return products.findAll().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return ProductResponse.from(find(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Change target price, threshold or pause tracking")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        TrackedProduct product = find(id);
        if (request.targetPrice() != null) {
            product.setTargetPriceKopecks(Money.toKopecks(request.targetPrice()));
        }
        if (request.dropThresholdPercent() != null) {
            product.setDropThresholdPercent(request.dropThresholdPercent());
        }
        if (request.active() != null) {
            product.setActive(request.active());
        }
        return ProductResponse.from(products.save(product));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        products.delete(find(id));
    }

    @PostMapping("/{id}/check")
    @Operation(summary = "Check the price right now instead of waiting for the scheduler")
    public ProductResponse checkNow(@PathVariable Long id) {
        find(id);
        checker.check(id);
        return ProductResponse.from(find(id));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Price history, oldest first")
    public List<PricePoint> history(@PathVariable Long id) {
        find(id);
        return snapshots.findAllByProductIdOrderByCapturedAtAsc(id).stream().map(PricePoint::from).toList();
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Min / max / current price over the whole history")
    public PriceStats stats(@PathVariable Long id) {
        TrackedProduct product = find(id);
        List<PriceSnapshot> history = snapshots.findAllByProductIdOrderByCapturedAtAsc(id);
        if (history.isEmpty()) {
            return new PriceStats(null, null, null, 0);
        }
        LongSummaryStatistics stats = history.stream().mapToLong(PriceSnapshot::getPriceKopecks).summaryStatistics();
        return new PriceStats(
                Money.toRubles(stats.getMin()),
                Money.toRubles(stats.getMax()),
                Money.toRublesOrNull(product.getLastPriceKopecks()),
                history.size());
    }

    private TrackedProduct find(Long id) {
        return products.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product " + id + " not found"));
    }
}
