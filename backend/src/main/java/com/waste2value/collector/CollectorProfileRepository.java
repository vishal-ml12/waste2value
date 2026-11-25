package com.waste2value.collector;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CollectorProfileRepository extends JpaRepository<CollectorProfile, Long> {

    Optional<CollectorProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Page<CollectorProfile> findByIsVerifiedTrue(Pageable pageable);

    Page<CollectorProfile> findByIsVerifiedFalse(Pageable pageable);

    long countByIsVerifiedTrue();
}
