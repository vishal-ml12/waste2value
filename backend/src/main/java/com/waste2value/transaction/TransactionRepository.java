package com.waste2value.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByPickupRequestId(Long pickupRequestId);

    Page<Transaction> findByGeneratorId(Long generatorId, Pageable pageable);

    Page<Transaction> findByCollectorId(Long collectorId, Pageable pageable);

    Page<Transaction> findByProcessorId(Long processorId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM Transaction t WHERE t.collector.id = :collectorId AND t.status = 'COMPLETED'")
    BigDecimal sumTotalAmountByCollectorId(@Param("collectorId") Long collectorId);

    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM Transaction t WHERE t.collector.id = :collectorId AND t.status = 'COMPLETED'")
    BigDecimal sumQuantityByCollectorId(@Param("collectorId") Long collectorId);

    long countByCollectorId(Long collectorId);
}
