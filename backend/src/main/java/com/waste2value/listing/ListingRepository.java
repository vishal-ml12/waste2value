package com.waste2value.listing;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Listing l WHERE l.id = :id")
    Optional<Listing> findByIdWithLock(@Param("id") Long id);

    Page<Listing> findByGeneratorId(Long generatorId, Pageable pageable);

    Page<Listing> findByGeneratorIdAndStatus(Long generatorId, ListingStatus status, Pageable pageable);

    long countByGeneratorIdAndStatus(Long generatorId, ListingStatus status);

    long countByStatus(ListingStatus status);

    long countByGeneratorId(Long generatorId);

    @Query("SELECT COALESCE(SUM(l.quantity), 0) FROM Listing l WHERE l.generator.id = :generatorId")
    BigDecimal sumQuantityByGeneratorId(@Param("generatorId") Long generatorId);

    List<Listing> findTop5ByGeneratorIdOrderByCreatedAtDesc(Long generatorId);
}
