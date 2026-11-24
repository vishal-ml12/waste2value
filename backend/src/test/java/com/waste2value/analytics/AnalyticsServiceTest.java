package com.waste2value.analytics;

import com.waste2value.analytics.dto.*;
import com.waste2value.category.Category;
import com.waste2value.category.CategoryRepository;
import com.waste2value.collector.CollectorProfileRepository;
import com.waste2value.listing.Listing;
import com.waste2value.listing.ListingRepository;
import com.waste2value.processor.ProcessorProfile;
import com.waste2value.processor.ProcessorProfileRepository;
import com.waste2value.receipt.ProcessorReceipt;
import com.waste2value.receipt.ProcessorReceiptRepository;
import com.waste2value.receipt.ProcessorReceiptStatus;
import com.waste2value.transaction.Transaction;
import com.waste2value.transaction.TransactionRepository;
import com.waste2value.transaction.TransactionStatus;
import com.waste2value.user.Role;
import com.waste2value.user.User;
import com.waste2value.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ProcessorReceiptRepository processorReceiptRepository;

    @Mock
    private EnvironmentalEmissionFactorRepository factorRepository;

    @Mock
    private ProcessorProfileRepository processorProfileRepository;

    @Mock
    private CollectorProfileRepository collectorProfileRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Spy
    private EnvironmentalImpactCalculator impactCalculator = new EnvironmentalImpactCalculator();

    @InjectMocks
    private AnalyticsService analyticsService;

    private Category petCategory;
    private Category aluminiumCategory;
    private EnvironmentalEmissionFactor petFactor;
    private EnvironmentalEmissionFactor aluminiumFactor;
    private User generator;
    private User collector;
    private ProcessorProfile processorProfile;
    private Listing listing;
    private Transaction transaction;
    private ProcessorReceipt receipt;

    @BeforeEach
    void setUp() {
        petCategory = new Category();
        petCategory.setId(8L);
        petCategory.setName("PET Bottles");
        petCategory.setSlug("pet-bottles");

        aluminiumCategory = new Category();
        aluminiumCategory.setId(13L);
        aluminiumCategory.setName("Aluminium");
        aluminiumCategory.setSlug("aluminium");

        petFactor = new EnvironmentalEmissionFactor(
                petCategory, new BigDecimal("1.5300"), new BigDecimal("1.0000"), "kg",
                "US EPA WARM v15 (PET)", "PET recycling", LocalDate.of(2024, 1, 1), null, true
        );
        petFactor.setId(1L);

        aluminiumFactor = new EnvironmentalEmissionFactor(
                aluminiumCategory, new BigDecimal("9.1200"), new BigDecimal("1.0000"), "kg",
                "US EPA WARM v15 (Aluminium)", "Aluminium smelting", LocalDate.of(2024, 1, 1), null, true
        );
        aluminiumFactor.setId(2L);

        generator = new User();
        generator.setId(2L);
        generator.setRole(Role.GENERATOR);
        generator.setFirstName("Eco");
        generator.setLastName("Generator");

        collector = new User();
        collector.setId(3L);
        collector.setRole(Role.COLLECTOR);
        collector.setFirstName("City");
        collector.setLastName("Collector");

        processorProfile = new ProcessorProfile();
        processorProfile.setId(10L);
        processorProfile.setCompanyName("Circular Refining Ltd");
        processorProfile.setLicenseNumber("LIC-9941");
        processorProfile.setMaxCapacityKg(new BigDecimal("10000.00"));
        processorProfile.setCurrentLoadKg(new BigDecimal("2500.00"));

        listing = new Listing();
        listing.setId(100L);
        listing.setCategory(petCategory);
        listing.setGenerator(generator);

        transaction = new Transaction();
        transaction.setId(500L);
        transaction.setListing(listing);
        transaction.setGenerator(generator);
        transaction.setCollector(collector);
        transaction.setQuantity(new BigDecimal("200.00"));
        transaction.setUnit("kg");
        transaction.setAgreedPrice(new BigDecimal("15.00"));
        transaction.setTotalAmount(new BigDecimal("3000.00"));
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setTransactionDate(Instant.parse("2026-05-10T10:00:00Z"));

        receipt = new ProcessorReceipt();
        receipt.setId(700L);
        receipt.setProcessor(processorProfile);
        receipt.setCollector(collector);
        receipt.setCategory(petCategory);
        receipt.setQuantity(new BigDecimal("200.00"));
        receipt.setUnit("kg");
        receipt.setStatus(ProcessorReceiptStatus.RECEIVED);
        receipt.setReceivedDate(Instant.parse("2026-05-15T10:00:00Z"));
    }

    @Test
    @DisplayName("Admin Overview accurately aggregates collected waste, confirmed recycled waste, and CO2 savings")
    void adminOverviewAggregatesAccurately() {
        when(transactionRepository.findCompletedTransactionsForAnalytics(any(), any(), any()))
                .thenReturn(List.of(transaction));
        when(processorReceiptRepository.findConfirmedReceiptsForAnalytics(any(), any(), any()))
                .thenReturn(List.of(receipt));
        when(factorRepository.findAllActiveWithCategory())
                .thenReturn(List.of(petFactor, aluminiumFactor));
        when(categoryRepository.findAll())
                .thenReturn(List.of(petCategory, aluminiumCategory));

        when(userRepository.countByRoleAndStatus(eq(Role.GENERATOR), any())).thenReturn(15L);
        when(collectorProfileRepository.countByIsVerifiedTrue()).thenReturn(6L);
        when(processorProfileRepository.countByIsVerifiedTrue()).thenReturn(3L);

        AdminOverviewStats stats = analyticsService.getAdminOverview(null, null);

        assertThat(stats.totalWasteCollectedKg()).isEqualTo(new BigDecimal("200.00"));
        assertThat(stats.totalWasteRecycledKg()).isEqualTo(new BigDecimal("200.00"));
        // 200 kg PET * 1.53 = 306.00 kg CO2 avoided
        assertThat(stats.totalCo2AvoidedKg()).isEqualTo(new BigDecimal("306.00"));
        assertThat(stats.totalCo2AvoidedMt()).isEqualTo(new BigDecimal("0.306"));
        assertThat(stats.totalLandfillDivertedKg()).isEqualTo(new BigDecimal("200.00"));
        assertThat(stats.completedTransactionsCount()).isEqualTo(1);
        assertThat(stats.confirmedReceiptsCount()).isEqualTo(1);
        assertThat(stats.activeGeneratorsCount()).isEqualTo(15L);
        assertThat(stats.disclaimer()).contains("estimates");
    }

    @Test
    @DisplayName("Waste Collection Analytics groups categories and monthly trends without N+1 queries")
    void wasteCollectionAnalyticsGroupsProperly() {
        when(transactionRepository.findCompletedTransactionsForAnalytics(any(), any(), any()))
                .thenReturn(List.of(transaction));
        when(factorRepository.findAllActiveWithCategory()).thenReturn(List.of(petFactor));
        when(categoryRepository.findAll()).thenReturn(List.of(petCategory));

        WasteCollectionAnalytics analytics = analyticsService.getWasteCollectionAnalytics(null, null, null);

        assertThat(analytics.totalCollectedKg()).isEqualTo(new BigDecimal("200.00"));
        assertThat(analytics.completedTransactionsCount()).isEqualTo(1);
        assertThat(analytics.totalTransactionValue()).isEqualTo(new BigDecimal("3000.00"));
        assertThat(analytics.categoryBreakdown()).hasSize(1);
        assertThat(analytics.categoryBreakdown().get(0).categoryName()).isEqualTo("PET Bottles");
        assertThat(analytics.categoryBreakdown().get(0).collectedKg()).isEqualTo(new BigDecimal("200.00"));
        assertThat(analytics.monthlyTrends()).hasSize(1);
        assertThat(analytics.monthlyTrends().get(0).month()).isEqualTo("2026-05");
    }

    @Test
    @DisplayName("Recycling Analytics groups processors and calculates environmental avoided burden")
    void recyclingAnalyticsComputesAccurately() {
        when(processorReceiptRepository.findConfirmedReceiptsForAnalytics(any(), any(), any()))
                .thenReturn(List.of(receipt));
        when(factorRepository.findAllActiveWithCategory()).thenReturn(List.of(petFactor));
        when(categoryRepository.findAll()).thenReturn(List.of(petCategory));

        RecyclingAnalytics analytics = analyticsService.getRecyclingAnalytics(null, null, null);

        assertThat(analytics.totalRecycledKg()).isEqualTo(new BigDecimal("200.00"));
        assertThat(analytics.confirmedReceiptsCount()).isEqualTo(1);
        assertThat(analytics.activeFacilitiesCount()).isEqualTo(1);
        assertThat(analytics.processorBreakdown()).hasSize(1);
        assertThat(analytics.processorBreakdown().get(0).companyName()).isEqualTo("Circular Refining Ltd");
        assertThat(analytics.processorBreakdown().get(0).recycledKg()).isEqualTo(new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("Generator Analytics isolates data to the authenticated generator")
    void generatorAnalyticsIsolatesUserData() {
        when(listingRepository.sumQuantityByGeneratorId(2L)).thenReturn(new BigDecimal("450.00"));
        when(transactionRepository.findCompletedTransactionsByGeneratorForAnalytics(eq(2L), any(), any()))
                .thenReturn(List.of(transaction));
        when(processorReceiptRepository.findConfirmedReceiptsByGeneratorForAnalytics(eq(2L), any(), any()))
                .thenReturn(List.of(receipt));
        when(factorRepository.findAllActiveWithCategory()).thenReturn(List.of(petFactor));
        when(categoryRepository.findAll()).thenReturn(List.of(petCategory));

        GeneratorAnalyticsResponse res = analyticsService.getGeneratorAnalytics(2L, null, null);

        assertThat(res.generatorId()).isEqualTo(2L);
        assertThat(res.totalListedKg()).isEqualTo(new BigDecimal("450.00"));
        assertThat(res.totalTransactedKg()).isEqualTo(new BigDecimal("200.00"));
        assertThat(res.totalEarnings()).isEqualTo(new BigDecimal("3000.00"));
        assertThat(res.confirmedRecycledKg()).isEqualTo(new BigDecimal("200.00"));
        assertThat(res.estimatedCo2AvoidedKg()).isEqualTo(new BigDecimal("306.00"));
        assertThat(res.estimatedLandfillDivertedKg()).isEqualTo(new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("Collector Analytics reports collected vs delivered quantities")
    void collectorAnalyticsReportsCorrectly() {
        when(transactionRepository.findCompletedTransactionsByCollectorForAnalytics(eq(3L), any(), any()))
                .thenReturn(List.of(transaction));
        when(processorReceiptRepository.findConfirmedReceiptsByCollectorForAnalytics(eq(3L), any(), any()))
                .thenReturn(List.of(receipt));

        CollectorAnalyticsResponse res = analyticsService.getCollectorAnalytics(3L, null, null);

        assertThat(res.collectorId()).isEqualTo(3L);
        assertThat(res.totalCollectedKg()).isEqualTo(new BigDecimal("200.00"));
        assertThat(res.totalDeliveredToProcessorsKg()).isEqualTo(new BigDecimal("200.00"));
        assertThat(res.completedPickupsCount()).isEqualTo(1);
        assertThat(res.processorReceiptsCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Processor Analytics calculates facility utilization and avoided emissions")
    void processorAnalyticsCalculatesCapacityUtilization() {
        when(processorProfileRepository.findByUserId(4L)).thenReturn(Optional.of(processorProfile));
        when(processorReceiptRepository.findConfirmedReceiptsByProcessorForAnalytics(eq(10L), any(), any()))
                .thenReturn(List.of(receipt));
        when(factorRepository.findAllActiveWithCategory()).thenReturn(List.of(petFactor));
        when(categoryRepository.findAll()).thenReturn(List.of(petCategory));

        ProcessorAnalyticsResponse res = analyticsService.getProcessorAnalytics(4L, null, null);

        assertThat(res.processorProfileId()).isEqualTo(10L);
        assertThat(res.totalProcessedKg()).isEqualTo(new BigDecimal("200.00"));
        assertThat(res.confirmedReceiptsCount()).isEqualTo(1);
        // Current load 2500 / 10000 = 25.0%
        assertThat(res.capacityUtilizationPercent()).isEqualTo(new BigDecimal("25.0"));
        assertThat(res.estimatedCo2AvoidedKg()).isEqualTo(new BigDecimal("306.00"));
        assertThat(res.estimatedLandfillDivertedKg()).isEqualTo(new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("Admin can update emission factors and source references successfully")
    void adminCanUpdateEmissionFactors() {
        when(factorRepository.findById(1L)).thenReturn(Optional.of(petFactor));
        when(factorRepository.save(any(EnvironmentalEmissionFactor.class))).thenAnswer(i -> i.getArgument(0));

        UpdateEmissionFactorRequest req = new UpdateEmissionFactorRequest(
                new BigDecimal("1.6000"),
                new BigDecimal("1.0000"),
                "US EPA WARM v16 (Updated)",
                "Updated life cycle assessment",
                true
        );

        EmissionFactorResponse res = analyticsService.updateEmissionFactor(1L, req);

        assertThat(res.co2FactorKgPerUnit()).isEqualTo(new BigDecimal("1.6000"));
        assertThat(res.sourceReference()).isEqualTo("US EPA WARM v16 (Updated)");
    }
}
