package com.waste2value.integration;

import com.waste2value.address.Address;
import com.waste2value.address.AddressRepository;
import com.waste2value.analytics.AnalyticsService;
import com.waste2value.analytics.EnvironmentalEmissionFactor;
import com.waste2value.analytics.EnvironmentalEmissionFactorRepository;
import com.waste2value.analytics.dto.EnvironmentalImpactResponse;
import com.waste2value.analytics.dto.RecyclingAnalytics;
import com.waste2value.analytics.dto.WasteCollectionAnalytics;
import com.waste2value.category.Category;
import com.waste2value.category.CategoryRepository;
import com.waste2value.collector.CollectorProfile;
import com.waste2value.collector.CollectorProfileRepository;
import com.waste2value.listing.Listing;
import com.waste2value.listing.ListingRepository;
import com.waste2value.listing.ListingStatus;
import com.waste2value.matching.MatchingService;
import com.waste2value.matching.dto.ListingRecommendationResponse;
import com.waste2value.pickup.PickupRequest;
import com.waste2value.pickup.PickupRequestRepository;
import com.waste2value.pickup.PickupRequestService;
import com.waste2value.pickup.PickupRequestStatus;
import com.waste2value.pickup.dto.CompletePickupRequest;
import com.waste2value.pickup.dto.CreatePickupRequest;
import com.waste2value.pickup.dto.PickupRequestResponse;
import com.waste2value.pickup.dto.RespondPickupRequest;
import com.waste2value.processor.ProcessorCapability;
import com.waste2value.processor.ProcessorCapabilityRepository;
import com.waste2value.processor.ProcessorProfile;
import com.waste2value.processor.ProcessorProfileRepository;
import com.waste2value.receipt.ProcessorReceiptRepository;
import com.waste2value.receipt.ProcessorReceiptService;
import com.waste2value.receipt.ProcessorReceiptStatus;
import com.waste2value.receipt.dto.CreateProcessorReceiptRequest;
import com.waste2value.receipt.dto.ProcessorReceiptResponse;
import com.waste2value.routing.RouteService;
import com.waste2value.routing.dto.PickupStopResponse;
import com.waste2value.routing.dto.RoutePlanResponse;
import com.waste2value.transaction.TransactionRepository;
import com.waste2value.user.Role;
import com.waste2value.user.User;
import com.waste2value.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FullSystemLifecycleIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private CollectorProfileRepository collectorProfileRepository;

    @Autowired
    private ProcessorProfileRepository processorProfileRepository;

    @Autowired
    private ProcessorCapabilityRepository processorCapabilityRepository;

    @Autowired
    private PickupRequestRepository pickupRequestRepository;

    @Autowired
    private PickupRequestService pickupRequestService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ProcessorReceiptRepository processorReceiptRepository;

    @Autowired
    private ProcessorReceiptService processorReceiptService;

    @Autowired
    private EnvironmentalEmissionFactorRepository factorRepository;

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private RouteService routeService;

    @Autowired
    private AnalyticsService analyticsService;

    private User generator;
    private User collector;
    private User processorUser;
    private Category category;
    private Address generatorAddress;
    private Address collectorDepot;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);

        // 1. Create Generator
        generator = new User("GenFirst", "GenLast", "gen_" + uid + "@example.com", "hash", "9876543210", Role.GENERATOR);
        generator = userRepository.save(generator);

        generatorAddress = new Address(generator, "10 Industrial Hub", null, "Mumbai", "Maharashtra", "400072",
                new BigDecimal("19.1136"), new BigDecimal("72.8697"), true);
        generatorAddress = addressRepository.save(generatorAddress);

        // 2. Create Collector & Profile
        collector = new User("ColFirst", "ColLast", "col_" + uid + "@example.com", "hash", "9876543211", Role.COLLECTOR);
        collector = userRepository.save(collector);

        collectorDepot = new Address(collector, "Central Logistics Base", null, "Mumbai", "Maharashtra", "400017",
                new BigDecimal("19.0434"), new BigDecimal("72.8634"), true);
        collectorDepot = addressRepository.save(collectorDepot);

        CollectorProfile colProfile = new CollectorProfile();
        colProfile.setUser(collector);
        colProfile.setVehicleType("Flatbed 5-Ton Truck");
        colProfile.setServiceArea("Mumbai Metropolitan Region");
        colProfile.setMaxDistanceKm(new BigDecimal("50.00"));
        colProfile.setVerified(true);
        colProfile.setAvgRating(new BigDecimal("5.00"));
        collectorProfileRepository.save(colProfile);

        // 3. Create Processor & Profile
        processorUser = new User("ProcFirst", "ProcLast", "proc_" + uid + "@example.com", "hash", "9876543212", Role.PROCESSOR);
        processorUser = userRepository.save(processorUser);

        ProcessorProfile procProfile = new ProcessorProfile();
        procProfile.setUser(processorUser);
        procProfile.setCompanyName("EcoCircle Materials Recovery");
        procProfile.setLicenseNumber("MPCB-REC-" + uid);
        procProfile.setMaxCapacityKg(new BigDecimal("15000.00"));
        procProfile.setCurrentLoadKg(BigDecimal.ZERO);
        procProfile.setVerified(true);
        procProfile.setAvgRating(new BigDecimal("5.00"));
        procProfile = processorProfileRepository.save(procProfile);

        // 4. Create Category
        category = new Category(null, "Plastics", "plastics-" + uid, "Recyclable industrial polymers", "kg", true);
        category = categoryRepository.save(category);

        // 5. Create Processor Capability
        ProcessorCapability cap = new ProcessorCapability();
        cap.setProcessor(procProfile);
        cap.setCategory(category);
        cap.setMinQuantity(new BigDecimal("10.00"));
        cap.setMaxQuantity(new BigDecimal("5000.00"));
        cap.setPricePerUnit(new BigDecimal("15.00"));
        cap.setActive(true);
        processorCapabilityRepository.save(cap);

        // 6. Seed Environmental Emission Factor (US EPA WARM: 1.43 kg CO2e / kg)
        EnvironmentalEmissionFactor factor = new EnvironmentalEmissionFactor(
                category, new BigDecimal("1.4300"), new BigDecimal("1.00"), "kg",
                "US EPA WARM v15 (2020)", "Closed-loop polymer offset",
                LocalDate.now().minusMonths(1), null, true
        );
        factorRepository.save(factor);
    }

    @Test
    @DisplayName("Complete System Integration: Generator Listing -> Matching -> Pickup -> Route Planning -> Completion -> Processing -> Analytics")
    void testEndToEndCircularWasteWorkflow() {
        // STEP 1: Generator creates and publishes listing (100 kg plastics)
        Listing newListing = new Listing(
                generator, category, generatorAddress, "Clean HDPE Flakes",
                "Industrial regrind flakes ready for collection",
                new BigDecimal("100.00"), "kg", new BigDecimal("1200.00"), ListingStatus.POSTED
        );
        final Listing listing = listingRepository.save(newListing);
        assertThat(listing.getId()).isNotNull();
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.POSTED);

        // STEP 2: Collector discovers listing via Phase 3A Smart Matching
        final Long expectedListingId = listing.getId();
        List<ListingRecommendationResponse> recommendations = matchingService.getRecommendedListingsForCollector(collector.getId());
        assertThat(recommendations).isNotEmpty();
        boolean foundListing = recommendations.stream().anyMatch(r -> r.listingId().equals(expectedListingId));
        assertThat(foundListing).isTrue();

        // STEP 3: Collector submits pickup request
        CreatePickupRequest createReq = new CreatePickupRequest(new BigDecimal("1200.00"), "Ready for pickup tomorrow");
        PickupRequestResponse pickupResp = pickupRequestService.createPickupRequest(collector.getId(), listing.getId(), createReq);
        assertThat(pickupResp.status()).isEqualTo(PickupRequestStatus.REQUESTED);
        Long pickupRequestId = pickupResp.id();

        // STEP 4: Generator accepts pickup request
        PickupRequestResponse acceptedResp = pickupRequestService.respondToRequest(
                generator.getId(), pickupRequestId,
                new RespondPickupRequest(true, "Accepted for collection")
        );
        assertThat(acceptedResp.status()).isEqualTo(PickupRequestStatus.ACCEPTED);

        // STEP 5: Collector sees accepted pickup in Phase 3C Route Planning
        RoutePlanResponse routePlan = routeService.getRoutePlan(collector.getId(), null);
        assertThat(routePlan).isNotNull();
        assertThat(routePlan.collectorId()).isEqualTo(collector.getId());
        assertThat(routePlan.totalStopsCount()).isEqualTo(1);
        assertThat(routePlan.mappableStopsCount()).isEqualTo(1);
        assertThat(routePlan.totalEstimatedDistanceKm()).isGreaterThan(0.0);

        PickupStopResponse stop1 = routePlan.stops().get(0);
        assertThat(stop1.stopSequence()).isEqualTo(1);
        assertThat(stop1.pickupRequestId()).isEqualTo(pickupRequestId);
        assertThat(stop1.listingTitle()).isEqualTo("Clean HDPE Flakes");
        assertThat(stop1.hasValidCoordinates()).isTrue();
        assertThat(stop1.legDistanceKm()).isNotNull();

        // STEP 6: Collector completes pickup (existing workflow unchanged)
        CompletePickupRequest compReq = new CompletePickupRequest(new BigDecimal("100.00"), "Material loaded and weighed on site");
        PickupRequestResponse completedPickup = pickupRequestService.completePickup(collector.getId(), pickupRequestId, compReq);
        assertThat(completedPickup.status()).isEqualTo(PickupRequestStatus.COMPLETED);

        // Verify transaction created
        var txOpt = transactionRepository.findByPickupRequestId(pickupRequestId);
        assertThat(txOpt).isPresent();
        assertThat(txOpt.get().getQuantity()).isEqualByComparingTo("100.00");
        assertThat(txOpt.get().getStatus().name()).isEqualTo("COMPLETED");

        // STEP 7: Route planning updates (completed pickup is no longer an active stop)
        RoutePlanResponse updatedPlan = routeService.getRoutePlan(collector.getId(), null);
        assertThat(updatedPlan.totalStopsCount()).isEqualTo(0);

        // STEP 8: Processor receives material intake from collector
        CreateProcessorReceiptRequest receiptReq = new CreateProcessorReceiptRequest(
                collector.getId(),
                category.getId(),
                listing.getId(),
                pickupRequestId,
                new BigDecimal("100.00"),
                "kg",
                new BigDecimal("15.00"),
                "Batch #HDPE-902 received at plant dock"
        );
        ProcessorReceiptResponse receiptResp = processorReceiptService.recordReceipt(processorUser.getId(), receiptReq);
        assertThat(receiptResp.status()).isEqualTo(ProcessorReceiptStatus.RECEIVED);

        // STEP 9: Phase 3B Analytics updates authoritatively
        WasteCollectionAnalytics collectionAnalytics = analyticsService.getWasteCollectionAnalytics(null, null, null);
        assertThat(collectionAnalytics.totalCollectedKg()).isGreaterThanOrEqualTo(new BigDecimal("100.00"));

        RecyclingAnalytics recyclingAnalytics = analyticsService.getRecyclingAnalytics(null, null, null);
        assertThat(recyclingAnalytics.totalRecycledKg()).isGreaterThanOrEqualTo(new BigDecimal("100.00"));

        EnvironmentalImpactResponse impactResponse = analyticsService.getEnvironmentalImpact(null, null, null);
        assertThat(impactResponse.totalRecycledKg()).isGreaterThanOrEqualTo(new BigDecimal("100.00"));
        assertThat(impactResponse.totalLandfillDivertedKg()).isGreaterThanOrEqualTo(new BigDecimal("100.00"));
        // 100 kg * 1.43 kg CO2e/kg = 143.0 kg CO2e avoided
        assertThat(impactResponse.totalCo2AvoidedKg()).isGreaterThanOrEqualTo(new BigDecimal("143.00"));
    }
}
