package com.waste2value.processor;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessorProfileRepository extends JpaRepository<ProcessorProfile, Long> {

    Optional<ProcessorProfile> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProcessorProfile p WHERE p.user.id = :userId")
    Optional<ProcessorProfile> findByUserIdWithLock(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProcessorProfile p WHERE p.id = :id")
    Optional<ProcessorProfile> findByIdWithLock(@Param("id") Long id);

    boolean existsByUserId(Long userId);

    Page<ProcessorProfile> findByIsVerifiedTrue(Pageable pageable);

    Page<ProcessorProfile> findByIsVerifiedFalse(Pageable pageable);

    long countByIsVerifiedTrue();
}
