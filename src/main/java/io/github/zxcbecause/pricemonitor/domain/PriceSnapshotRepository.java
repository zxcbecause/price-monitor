package io.github.zxcbecause.pricemonitor.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {

    List<PriceSnapshot> findAllByProductIdOrderByCapturedAtAsc(Long productId);
}
