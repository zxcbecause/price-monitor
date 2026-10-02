package io.github.zxcbecause.pricemonitor.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackedProductRepository extends JpaRepository<TrackedProduct, Long> {

    boolean existsByMarketplaceAndSku(Marketplace marketplace, String sku);

    List<TrackedProduct> findAllByActiveTrue();
}
