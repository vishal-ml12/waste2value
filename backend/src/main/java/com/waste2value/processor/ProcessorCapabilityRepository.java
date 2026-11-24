package com.waste2value.processor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessorCapabilityRepository extends JpaRepository<ProcessorCapability, Long> {

    List<ProcessorCapability> findByProcessorId(Long processorId);

    Optional<ProcessorCapability> findByProcessorIdAndCategoryId(Long processorId, Long categoryId);

    boolean existsByProcessorIdAndCategoryId(Long processorId, Long categoryId);

    List<ProcessorCapability> findByCategoryIdAndIsActiveTrue(Long categoryId);
}
