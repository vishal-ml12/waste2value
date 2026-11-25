package com.waste2value.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnvironmentalEmissionFactorRepository extends JpaRepository<EnvironmentalEmissionFactor, Long> {

    @Query("SELECT e FROM EnvironmentalEmissionFactor e JOIN FETCH e.category c ORDER BY c.id ASC")
    List<EnvironmentalEmissionFactor> findAllWithCategory();

    @Query("SELECT e FROM EnvironmentalEmissionFactor e JOIN FETCH e.category c WHERE e.isActive = true ORDER BY c.id ASC")
    List<EnvironmentalEmissionFactor> findAllActiveWithCategory();

    @Query("SELECT e FROM EnvironmentalEmissionFactor e WHERE e.category.id = :categoryId AND e.isActive = true " +
           "AND e.effectiveFrom <= :date AND (e.effectiveTo IS NULL OR e.effectiveTo >= :date) " +
           "ORDER BY e.effectiveFrom DESC")
    List<EnvironmentalEmissionFactor> findEffectiveFactorsForCategory(@Param("categoryId") Long categoryId, @Param("date") LocalDate date);

    Optional<EnvironmentalEmissionFactor> findFirstByCategoryIdAndIsActiveTrueOrderByEffectiveFromDesc(Long categoryId);
}
