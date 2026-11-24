package com.waste2value.receipt;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessorReceiptRepository extends JpaRepository<ProcessorReceipt, Long> {

    Page<ProcessorReceipt> findByProcessorId(Long processorId, Pageable pageable);

    Page<ProcessorReceipt> findByCollectorId(Long collectorId, Pageable pageable);

    long countByProcessorId(Long processorId);
}
