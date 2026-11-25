package com.waste2value.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM Transaction t WHERE t.generator.id = :generatorId AND t.status = 'COMPLETED'")
    BigDecimal sumQuantityByGeneratorId(@Param("generatorId") Long generatorId);

    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM Transaction t WHERE t.generator.id = :generatorId AND t.status = 'COMPLETED'")
    BigDecimal sumTotalAmountByGeneratorId(@Param("generatorId") Long generatorId);

    long countByGeneratorIdAndStatus(Long generatorId, TransactionStatus status);

    @Query("SELECT t FROM Transaction t " +
           "JOIN FETCH t.listing l " +
           "JOIN FETCH l.category c " +
           "WHERE t.status = com.waste2value.transaction.TransactionStatus.COMPLETED " +
           "AND (:startDate IS NULL OR t.transactionDate >= :startDate) " +
           "AND (:endDate IS NULL OR t.transactionDate <= :endDate) " +
           "AND (:categoryId IS NULL OR c.id = :categoryId OR c.parentCategory.id = :categoryId) " +
           "ORDER BY t.transactionDate ASC")
    List<Transaction> findCompletedTransactionsForAnalytics(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("categoryId") Long categoryId
    );

    @Query("SELECT t FROM Transaction t " +
           "JOIN FETCH t.listing l " +
           "JOIN FETCH l.category c " +
           "WHERE t.status = com.waste2value.transaction.TransactionStatus.COMPLETED " +
           "AND t.generator.id = :generatorId " +
           "AND (:startDate IS NULL OR t.transactionDate >= :startDate) " +
           "AND (:endDate IS NULL OR t.transactionDate <= :endDate) " +
           "ORDER BY t.transactionDate ASC")
    List<Transaction> findCompletedTransactionsByGeneratorForAnalytics(
            @Param("generatorId") Long generatorId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    @Query("SELECT t FROM Transaction t " +
           "JOIN FETCH t.listing l " +
           "JOIN FETCH l.category c " +
           "WHERE t.status = com.waste2value.transaction.TransactionStatus.COMPLETED " +
           "AND t.collector.id = :collectorId " +
           "AND (:startDate IS NULL OR t.transactionDate >= :startDate) " +
           "AND (:endDate IS NULL OR t.transactionDate <= :endDate) " +
           "ORDER BY t.transactionDate ASC")
    List<Transaction> findCompletedTransactionsByCollectorForAnalytics(
            @Param("collectorId") Long collectorId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
}
