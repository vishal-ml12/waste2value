package com.waste2value.pickup;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PickupRequestRepository extends JpaRepository<PickupRequest, Long> {

    List<PickupRequest> findByListingId(Long listingId);

    Page<PickupRequest> findByCollectorId(Long collectorId, Pageable pageable);

    Page<PickupRequest> findByCollectorIdAndStatus(Long collectorId, PickupRequestStatus status, Pageable pageable);

    Page<PickupRequest> findByListingGeneratorId(Long generatorId, Pageable pageable);

    Page<PickupRequest> findByListingGeneratorIdAndStatus(Long generatorId, PickupRequestStatus status, Pageable pageable);

    Optional<PickupRequest> findByListingIdAndCollectorIdAndStatusIn(Long listingId, Long collectorId, List<PickupRequestStatus> statuses);

    List<PickupRequest> findByListingIdAndStatus(Long listingId, PickupRequestStatus status);

    long countByCollectorIdAndStatus(Long collectorId, PickupRequestStatus status);

    long countByListingGeneratorIdAndStatus(Long generatorId, PickupRequestStatus status);

    long countByStatus(PickupRequestStatus status);

    @Query("SELECT pr FROM PickupRequest pr WHERE pr.listing.id = :listingId AND pr.status = 'ACCEPTED'")
    Optional<PickupRequest> findAcceptedRequestByListingId(@Param("listingId") Long listingId);

    @Query("SELECT pr FROM PickupRequest pr " +
           "JOIN FETCH pr.listing l " +
           "JOIN FETCH l.address a " +
           "JOIN FETCH l.category c " +
           "JOIN FETCH l.generator g " +
           "WHERE pr.collector.id = :collectorId AND pr.status = 'ACCEPTED' " +
           "ORDER BY pr.requestedAt ASC")
    List<PickupRequest> findAcceptedPickupsForCollectorWithDetails(@Param("collectorId") Long collectorId);
}
