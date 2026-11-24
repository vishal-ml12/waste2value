package com.waste2value.receipt;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ProcessorReceiptRepository extends JpaRepository<ProcessorReceipt, Long> {

    Page<ProcessorReceipt> findByProcessorId(Long processorId, Pageable pageable);

    Page<ProcessorReceipt> findByCollectorId(Long collectorId, Pageable pageable);

    long countByProcessorId(Long processorId);

    @Query("SELECT r FROM ProcessorReceipt r " +
           "JOIN FETCH r.category c " +
           "JOIN FETCH r.processor p " +
           "WHERE r.status IN (com.waste2value.receipt.ProcessorReceiptStatus.RECEIVED, com.waste2value.receipt.ProcessorReceiptStatus.VERIFIED) " +
           "AND (:startDate IS NULL OR r.receivedDate >= :startDate) " +
           "AND (:endDate IS NULL OR r.receivedDate <= :endDate) " +
           "AND (:categoryId IS NULL OR c.id = :categoryId OR c.parentCategory.id = :categoryId) " +
           "ORDER BY r.receivedDate ASC")
    List<ProcessorReceipt> findConfirmedReceiptsForAnalytics(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("categoryId") Long categoryId
    );

    @Query("SELECT r FROM ProcessorReceipt r " +
           "JOIN FETCH r.category c " +
           "JOIN FETCH r.processor p " +
           "WHERE r.status IN (com.waste2value.receipt.ProcessorReceiptStatus.RECEIVED, com.waste2value.receipt.ProcessorReceiptStatus.VERIFIED) " +
           "AND p.id = :processorProfileId " +
           "AND (:startDate IS NULL OR r.receivedDate >= :startDate) " +
           "AND (:endDate IS NULL OR r.receivedDate <= :endDate) " +
           "ORDER BY r.receivedDate ASC")
    List<ProcessorReceipt> findConfirmedReceiptsByProcessorForAnalytics(
            @Param("processorProfileId") Long processorProfileId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    @Query("SELECT r FROM ProcessorReceipt r " +
           "JOIN FETCH r.category c " +
           "JOIN FETCH r.processor p " +
           "WHERE r.status IN (com.waste2value.receipt.ProcessorReceiptStatus.RECEIVED, com.waste2value.receipt.ProcessorReceiptStatus.VERIFIED) " +
           "AND r.collector.id = :collectorId " +
           "AND (:startDate IS NULL OR r.receivedDate >= :startDate) " +
           "AND (:endDate IS NULL OR r.receivedDate <= :endDate) " +
           "ORDER BY r.receivedDate ASC")
    List<ProcessorReceipt> findConfirmedReceiptsByCollectorForAnalytics(
            @Param("collectorId") Long collectorId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    @Query("SELECT r FROM ProcessorReceipt r " +
           "JOIN FETCH r.category c " +
           "JOIN FETCH r.processor p " +
           "LEFT JOIN r.transaction t " +
           "LEFT JOIN r.listing l " +
           "WHERE r.status IN (com.waste2value.receipt.ProcessorReceiptStatus.RECEIVED, com.waste2value.receipt.ProcessorReceiptStatus.VERIFIED) " +
           "AND (t.generator.id = :generatorId OR l.generator.id = :generatorId) " +
           "AND (:startDate IS NULL OR r.receivedDate >= :startDate) " +
           "AND (:endDate IS NULL OR r.receivedDate <= :endDate) " +
           "ORDER BY r.receivedDate ASC")
    List<ProcessorReceipt> findConfirmedReceiptsByGeneratorForAnalytics(
            @Param("generatorId") Long generatorId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
}
