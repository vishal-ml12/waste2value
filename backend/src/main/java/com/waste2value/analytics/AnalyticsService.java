package com.waste2value.analytics;

import com.waste2value.analytics.dto.*;
import com.waste2value.category.Category;
import com.waste2value.category.CategoryRepository;
import com.waste2value.collector.CollectorProfileRepository;
import com.waste2value.listing.ListingRepository;
import com.waste2value.processor.ProcessorProfile;
import com.waste2value.processor.ProcessorProfileRepository;
import com.waste2value.receipt.ProcessorReceipt;
import com.waste2value.receipt.ProcessorReceiptRepository;
import com.waste2value.shared.exception.ResourceNotFoundException;
import com.waste2value.transaction.Transaction;
import com.waste2value.transaction.TransactionRepository;
import com.waste2value.user.Role;
import com.waste2value.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final TransactionRepository transactionRepository;
    private final ProcessorReceiptRepository processorReceiptRepository;
    private final EnvironmentalEmissionFactorRepository factorRepository;
    private final ProcessorProfileRepository processorProfileRepository;
    private final CollectorProfileRepository collectorProfileRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EnvironmentalImpactCalculator impactCalculator;

    public AnalyticsService(
            TransactionRepository transactionRepository,
            ProcessorReceiptRepository processorReceiptRepository,
            EnvironmentalEmissionFactorRepository factorRepository,
            ProcessorProfileRepository processorProfileRepository,
            CollectorProfileRepository collectorProfileRepository,
            ListingRepository listingRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            EnvironmentalImpactCalculator impactCalculator
    ) {
        this.transactionRepository = transactionRepository;
        this.processorReceiptRepository = processorReceiptRepository;
        this.factorRepository = factorRepository;
        this.processorProfileRepository = processorProfileRepository;
        this.collectorProfileRepository = collectorProfileRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.impactCalculator = impactCalculator;
    }

    /**
     * Platform-wide high-level operational and environmental overview for Admin.
     */
    public AdminOverviewStats getAdminOverview(Instant startDate, Instant endDate) {
        List<Transaction> transactions = transactionRepository.findCompletedTransactionsForAnalytics(startDate, endDate, null);
        List<ProcessorReceipt> receipts = processorReceiptRepository.findConfirmedReceiptsForAnalytics(startDate, endDate, null);

        BigDecimal totalCollectedKg = transactions.stream()
                .map(Transaction::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalRecycledKg = receipts.stream()
                .map(ProcessorReceipt::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Map<Long, EnvironmentalEmissionFactor> factorMap = loadEffectiveFactorMap();
        List<EnvironmentalImpactCalculator.MaterialBatch> batches = receipts.stream()
                .map(r -> new EnvironmentalImpactCalculator.MaterialBatch(
                        r.getCategory().getId(),
                        r.getCategory().getName(),
                        r.getQuantity()
                ))
                .toList();

        EnvironmentalImpactCalculator.ImpactSummary impact = impactCalculator.calculateImpact(batches, factorMap);

        long activeGenerators = userRepository.countByRoleAndStatus(Role.GENERATOR, com.waste2value.user.AccountStatus.ACTIVE);
        long activeCollectors = collectorProfileRepository.countByIsVerifiedTrue();
        long activeProcessors = processorProfileRepository.countByIsVerifiedTrue();

        return new AdminOverviewStats(
                totalCollectedKg,
                totalRecycledKg,
                impact.totalCo2AvoidedKg(),
                impact.totalCo2AvoidedMt(),
                impact.totalLandfillDivertedKg(),
                transactions.size(),
                receipts.size(),
                activeGenerators,
                activeCollectors,
                activeProcessors,
                EnvironmentalImpactCalculator.ESTIMATE_DISCLAIMER
        );
    }

    /**
     * Detailed waste collection analytics across all completed transactions.
     */
    public WasteCollectionAnalytics getWasteCollectionAnalytics(Instant startDate, Instant endDate, Long categoryId) {
        List<Transaction> transactions = transactionRepository.findCompletedTransactionsForAnalytics(startDate, endDate, categoryId);

        BigDecimal totalCollectedKg = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;

        Map<Long, CategoryAggregator> catMap = new LinkedHashMap<>();
        Map<String, MonthlyAggregator> monthMap = new TreeMap<>();

        for (Transaction t : transactions) {
            BigDecimal qty = t.getQuantity() != null ? t.getQuantity() : BigDecimal.ZERO;
            BigDecimal amt = t.getTotalAmount() != null ? t.getTotalAmount() : BigDecimal.ZERO;

            totalCollectedKg = totalCollectedKg.add(qty);
            totalValue = totalValue.add(amt);

            Category c = t.getListing().getCategory();
            catMap.computeIfAbsent(c.getId(), id -> new CategoryAggregator(c.getId(), c.getName(), c.getSlug()))
                    .addCollected(qty);

            String monthStr = formatMonth(t.getTransactionDate());
            monthMap.computeIfAbsent(monthStr, MonthlyAggregator::new)
                    .addCollection(qty);
        }

        Map<Long, EnvironmentalEmissionFactor> factorMap = loadEffectiveFactorMap();

        List<CategoryAnalyticsPoint> categoryBreakdown = catMap.values().stream()
                .map(ca -> {
                    EnvironmentalEmissionFactor factor = factorMap.get(ca.categoryId);
                    BigDecimal co2Factor = factor != null ? factor.getCo2FactorKgPerUnit() : null;
                    boolean isConfigured = co2Factor != null;
                    String source = factor != null ? factor.getSourceReference() : "Unconfigured";
                    return new CategoryAnalyticsPoint(
                            ca.categoryId,
                            ca.categoryName,
                            ca.categorySlug,
                            ca.collectedKg.setScale(2, RoundingMode.HALF_UP),
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            co2Factor,
                            null, // Not recycled yet, only collected
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            isConfigured,
                            source
                    );
                })
                .sorted(Comparator.comparing(CategoryAnalyticsPoint::collectedKg).reversed())
                .toList();

        List<MonthlyTrendPoint> monthlyTrends = monthMap.values().stream()
                .map(MonthlyAggregator::toPoint)
                .toList();

        return new WasteCollectionAnalytics(
                totalCollectedKg.setScale(2, RoundingMode.HALF_UP),
                transactions.size(),
                totalValue.setScale(2, RoundingMode.HALF_UP),
                categoryBreakdown,
                monthlyTrends,
                startDate,
                endDate
        );
    }

    /**
     * Detailed recycling analytics from confirmed processor receipts.
     */
    public RecyclingAnalytics getRecyclingAnalytics(Instant startDate, Instant endDate, Long categoryId) {
        List<ProcessorReceipt> receipts = processorReceiptRepository.findConfirmedReceiptsForAnalytics(startDate, endDate, categoryId);
        Map<Long, EnvironmentalEmissionFactor> factorMap = loadEffectiveFactorMap();

        BigDecimal totalRecycledKg = BigDecimal.ZERO;
        Set<Long> facilityIds = new HashSet<>();

        Map<Long, CategoryAggregator> catMap = new LinkedHashMap<>();
        Map<Long, ProcessorAggregator> procMap = new LinkedHashMap<>();
        Map<String, MonthlyAggregator> monthMap = new TreeMap<>();

        for (ProcessorReceipt r : receipts) {
            BigDecimal qty = r.getQuantity() != null ? r.getQuantity() : BigDecimal.ZERO;
            totalRecycledKg = totalRecycledKg.add(qty);

            ProcessorProfile p = r.getProcessor();
            facilityIds.add(p.getId());

            Category c = r.getCategory();
            catMap.computeIfAbsent(c.getId(), id -> new CategoryAggregator(c.getId(), c.getName(), c.getSlug()))
                    .addRecycled(qty);

            procMap.computeIfAbsent(p.getId(), id -> new ProcessorAggregator(p.getId(), p.getCompanyName(), p.getLicenseNumber()))
                    .addReceipt(qty);

            String monthStr = formatMonth(r.getReceivedDate());
            monthMap.computeIfAbsent(monthStr, MonthlyAggregator::new)
                    .addReceipt(qty);
        }

        List<CategoryAnalyticsPoint> categoryBreakdown = catMap.values().stream()
                .map(ca -> {
                    EnvironmentalEmissionFactor factor = factorMap.get(ca.categoryId);
                    BigDecimal co2Factor = factor != null ? factor.getCo2FactorKgPerUnit() : null;
                    boolean isConfigured = co2Factor != null;
                    BigDecimal diversionRate = factor != null && factor.getLandfillDiversionRate() != null
                            ? factor.getLandfillDiversionRate()
                            : BigDecimal.ONE;

                    BigDecimal co2Avoided = isConfigured
                            ? ca.recycledKg.multiply(co2Factor).setScale(2, RoundingMode.HALF_UP)
                            : null;
                    BigDecimal landfillDiverted = ca.recycledKg.multiply(diversionRate).setScale(2, RoundingMode.HALF_UP);
                    String source = factor != null ? factor.getSourceReference() : "Unconfigured";

                    return new CategoryAnalyticsPoint(
                            ca.categoryId,
                            ca.categoryName,
                            ca.categorySlug,
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            ca.recycledKg.setScale(2, RoundingMode.HALF_UP),
                            co2Factor,
                            co2Avoided,
                            landfillDiverted,
                            isConfigured,
                            source
                    );
                })
                .sorted(Comparator.comparing(CategoryAnalyticsPoint::recycledKg).reversed())
                .toList();

        List<ProcessorAnalyticsPoint> processorBreakdown = procMap.values().stream()
                .map(pa -> new ProcessorAnalyticsPoint(
                        pa.processorId,
                        pa.companyName,
                        pa.licenseNumber,
                        pa.recycledKg.setScale(2, RoundingMode.HALF_UP),
                        pa.receiptsCount,
                        null,
                        pa.recycledKg.setScale(2, RoundingMode.HALF_UP)
                ))
                .sorted(Comparator.comparing(ProcessorAnalyticsPoint::recycledKg).reversed())
                .toList();

        List<MonthlyTrendPoint> monthlyTrends = monthMap.values().stream()
                .map(MonthlyAggregator::toPoint)
                .toList();

        return new RecyclingAnalytics(
                totalRecycledKg.setScale(2, RoundingMode.HALF_UP),
                receipts.size(),
                facilityIds.size(),
                categoryBreakdown,
                processorBreakdown,
                monthlyTrends,
                startDate,
                endDate
        );
    }

    /**
     * Detailed environmental impact calculation summary for confirmed recycled materials.
     */
    public EnvironmentalImpactResponse getEnvironmentalImpact(Instant startDate, Instant endDate, Long categoryId) {
        List<ProcessorReceipt> receipts = processorReceiptRepository.findConfirmedReceiptsForAnalytics(startDate, endDate, categoryId);
        Map<Long, EnvironmentalEmissionFactor> factorMap = loadEffectiveFactorMap();

        List<EnvironmentalImpactCalculator.MaterialBatch> batches = receipts.stream()
                .map(r -> new EnvironmentalImpactCalculator.MaterialBatch(
                        r.getCategory().getId(),
                        r.getCategory().getName(),
                        r.getQuantity()
                ))
                .toList();

        EnvironmentalImpactCalculator.ImpactSummary summary = impactCalculator.calculateImpact(batches, factorMap);

        return new EnvironmentalImpactResponse(
                summary.totalProcessedKg(),
                summary.totalCo2AvoidedKg(),
                summary.totalCo2AvoidedMt(),
                summary.totalLandfillDivertedKg(),
                summary.categoryBreakdown(),
                summary.unconfiguredCategories(),
                summary.methodology(),
                summary.disclaimer()
        );
    }

    /**
     * Personalized environmental and waste analytics for the authenticated Generator.
     */
    public GeneratorAnalyticsResponse getGeneratorAnalytics(Long generatorUserId, Instant startDate, Instant endDate) {
        BigDecimal totalListedKg = listingRepository.sumQuantityByGeneratorId(generatorUserId);
        if (totalListedKg == null) totalListedKg = BigDecimal.ZERO;

        List<Transaction> transactions = transactionRepository.findCompletedTransactionsByGeneratorForAnalytics(generatorUserId, startDate, endDate);
        List<ProcessorReceipt> receipts = processorReceiptRepository.findConfirmedReceiptsByGeneratorForAnalytics(generatorUserId, startDate, endDate);

        BigDecimal totalTransactedKg = BigDecimal.ZERO;
        BigDecimal totalEarnings = BigDecimal.ZERO;
        Map<Long, CategoryAggregator> catMap = new LinkedHashMap<>();
        Map<String, MonthlyAggregator> monthMap = new TreeMap<>();

        for (Transaction t : transactions) {
            BigDecimal qty = t.getQuantity() != null ? t.getQuantity() : BigDecimal.ZERO;
            BigDecimal amt = t.getTotalAmount() != null ? t.getTotalAmount() : BigDecimal.ZERO;

            totalTransactedKg = totalTransactedKg.add(qty);
            totalEarnings = totalEarnings.add(amt);

            Category c = t.getListing().getCategory();
            catMap.computeIfAbsent(c.getId(), id -> new CategoryAggregator(c.getId(), c.getName(), c.getSlug()))
                    .addCollected(qty);

            String monthStr = formatMonth(t.getTransactionDate());
            monthMap.computeIfAbsent(monthStr, MonthlyAggregator::new)
                    .addCollection(qty);
        }

        Map<Long, EnvironmentalEmissionFactor> factorMap = loadEffectiveFactorMap();

        // Calculate confirmed environmental impact if material reached a processor facility
        List<EnvironmentalImpactCalculator.MaterialBatch> confirmedBatches = receipts.stream()
                .map(r -> new EnvironmentalImpactCalculator.MaterialBatch(
                        r.getCategory().getId(),
                        r.getCategory().getName(),
                        r.getQuantity()
                ))
                .toList();

        EnvironmentalImpactCalculator.ImpactSummary confirmedImpact = impactCalculator.calculateImpact(confirmedBatches, factorMap);

        List<CategoryAnalyticsPoint> categoryDistribution = catMap.values().stream()
                .map(ca -> {
                    EnvironmentalEmissionFactor factor = factorMap.get(ca.categoryId);
                    BigDecimal co2Factor = factor != null ? factor.getCo2FactorKgPerUnit() : null;
                    boolean isConfigured = co2Factor != null;
                    String source = factor != null ? factor.getSourceReference() : "Unconfigured";
                    return new CategoryAnalyticsPoint(
                            ca.categoryId,
                            ca.categoryName,
                            ca.categorySlug,
                            ca.collectedKg.setScale(2, RoundingMode.HALF_UP),
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            co2Factor,
                            null,
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            isConfigured,
                            source
                    );
                })
                .sorted(Comparator.comparing(CategoryAnalyticsPoint::collectedKg).reversed())
                .toList();

        List<MonthlyTrendPoint> monthlyTrends = monthMap.values().stream()
                .map(MonthlyAggregator::toPoint)
                .toList();

        return new GeneratorAnalyticsResponse(
                generatorUserId,
                totalListedKg.setScale(2, RoundingMode.HALF_UP),
                totalTransactedKg.setScale(2, RoundingMode.HALF_UP),
                transactions.size(),
                totalEarnings.setScale(2, RoundingMode.HALF_UP),
                categoryDistribution,
                confirmedImpact.totalProcessedKg(),
                confirmedImpact.totalCo2AvoidedKg(),
                confirmedImpact.totalCo2AvoidedMt(),
                confirmedImpact.totalLandfillDivertedKg(),
                monthlyTrends,
                EnvironmentalImpactCalculator.ESTIMATE_DISCLAIMER
        );
    }

    /**
     * Personalized collection performance and delivery analytics for the authenticated Collector.
     */
    public CollectorAnalyticsResponse getCollectorAnalytics(Long collectorUserId, Instant startDate, Instant endDate) {
        List<Transaction> transactions = transactionRepository.findCompletedTransactionsByCollectorForAnalytics(collectorUserId, startDate, endDate);
        List<ProcessorReceipt> receipts = processorReceiptRepository.findConfirmedReceiptsByCollectorForAnalytics(collectorUserId, startDate, endDate);

        BigDecimal totalCollectedKg = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        Map<Long, CategoryAggregator> catMap = new LinkedHashMap<>();
        Map<String, MonthlyAggregator> monthMap = new TreeMap<>();

        for (Transaction t : transactions) {
            BigDecimal qty = t.getQuantity() != null ? t.getQuantity() : BigDecimal.ZERO;
            BigDecimal amt = t.getTotalAmount() != null ? t.getTotalAmount() : BigDecimal.ZERO;

            totalCollectedKg = totalCollectedKg.add(qty);
            totalValue = totalValue.add(amt);

            Category c = t.getListing().getCategory();
            catMap.computeIfAbsent(c.getId(), id -> new CategoryAggregator(c.getId(), c.getName(), c.getSlug()))
                    .addCollected(qty);

            String monthStr = formatMonth(t.getTransactionDate());
            monthMap.computeIfAbsent(monthStr, MonthlyAggregator::new)
                    .addCollection(qty);
        }

        BigDecimal totalDeliveredKg = receipts.stream()
                .map(ProcessorReceipt::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        for (ProcessorReceipt r : receipts) {
            String monthStr = formatMonth(r.getReceivedDate());
            monthMap.computeIfAbsent(monthStr, MonthlyAggregator::new)
                    .addReceipt(r.getQuantity() != null ? r.getQuantity() : BigDecimal.ZERO);
        }

        List<CategoryAnalyticsPoint> categoryDistribution = catMap.values().stream()
                .map(ca -> new CategoryAnalyticsPoint(
                        ca.categoryId,
                        ca.categoryName,
                        ca.categorySlug,
                        ca.collectedKg.setScale(2, RoundingMode.HALF_UP),
                        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                        null,
                        null,
                        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                        false,
                        "Collection Source"
                ))
                .sorted(Comparator.comparing(CategoryAnalyticsPoint::collectedKg).reversed())
                .toList();

        List<MonthlyTrendPoint> monthlyTrends = monthMap.values().stream()
                .map(MonthlyAggregator::toPoint)
                .toList();

        return new CollectorAnalyticsResponse(
                collectorUserId,
                totalCollectedKg.setScale(2, RoundingMode.HALF_UP),
                transactions.size(),
                totalValue.setScale(2, RoundingMode.HALF_UP),
                totalDeliveredKg,
                receipts.size(),
                categoryDistribution,
                monthlyTrends
        );
    }

    /**
     * Personalized facility intake, recycling, and environmental impact analytics for the authenticated Processor.
     */
    public ProcessorAnalyticsResponse getProcessorAnalytics(Long processorUserId, Instant startDate, Instant endDate) {
        ProcessorProfile profile = processorProfileRepository.findByUserId(processorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Processor profile not found for current user"));

        List<ProcessorReceipt> receipts = processorReceiptRepository.findConfirmedReceiptsByProcessorForAnalytics(profile.getId(), startDate, endDate);
        Map<Long, EnvironmentalEmissionFactor> factorMap = loadEffectiveFactorMap();

        BigDecimal totalProcessedKg = BigDecimal.ZERO;
        Map<Long, CategoryAggregator> catMap = new LinkedHashMap<>();
        Map<String, MonthlyAggregator> monthMap = new TreeMap<>();

        List<EnvironmentalImpactCalculator.MaterialBatch> batches = new ArrayList<>();

        for (ProcessorReceipt r : receipts) {
            BigDecimal qty = r.getQuantity() != null ? r.getQuantity() : BigDecimal.ZERO;
            totalProcessedKg = totalProcessedKg.add(qty);

            Category c = r.getCategory();
            catMap.computeIfAbsent(c.getId(), id -> new CategoryAggregator(c.getId(), c.getName(), c.getSlug()))
                    .addRecycled(qty);

            batches.add(new EnvironmentalImpactCalculator.MaterialBatch(c.getId(), c.getName(), qty));

            String monthStr = formatMonth(r.getReceivedDate());
            monthMap.computeIfAbsent(monthStr, MonthlyAggregator::new)
                    .addReceipt(qty);
        }

        EnvironmentalImpactCalculator.ImpactSummary impact = impactCalculator.calculateImpact(batches, factorMap);

        BigDecimal maxCap = profile.getMaxCapacityKg();
        BigDecimal currentLoad = profile.getCurrentLoadKg();
        BigDecimal utilizationPercent = BigDecimal.ZERO;
        if (maxCap != null && maxCap.compareTo(BigDecimal.ZERO) > 0 && currentLoad != null) {
            utilizationPercent = currentLoad.divide(maxCap, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100.00"))
                    .setScale(1, RoundingMode.HALF_UP);
            if (utilizationPercent.compareTo(new BigDecimal("100.0")) > 0) {
                utilizationPercent = new BigDecimal("100.0");
            }
        }

        List<CategoryAnalyticsPoint> categoryDistribution = catMap.values().stream()
                .map(ca -> {
                    EnvironmentalEmissionFactor factor = factorMap.get(ca.categoryId);
                    BigDecimal co2Factor = factor != null ? factor.getCo2FactorKgPerUnit() : null;
                    boolean isConfigured = co2Factor != null;
                    BigDecimal diversionRate = factor != null && factor.getLandfillDiversionRate() != null
                            ? factor.getLandfillDiversionRate()
                            : BigDecimal.ONE;

                    BigDecimal co2Avoided = isConfigured
                            ? ca.recycledKg.multiply(co2Factor).setScale(2, RoundingMode.HALF_UP)
                            : null;
                    BigDecimal landfillDiverted = ca.recycledKg.multiply(diversionRate).setScale(2, RoundingMode.HALF_UP);
                    String source = factor != null ? factor.getSourceReference() : "Unconfigured";

                    return new CategoryAnalyticsPoint(
                            ca.categoryId,
                            ca.categoryName,
                            ca.categorySlug,
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            ca.recycledKg.setScale(2, RoundingMode.HALF_UP),
                            co2Factor,
                            co2Avoided,
                            landfillDiverted,
                            isConfigured,
                            source
                    );
                })
                .sorted(Comparator.comparing(CategoryAnalyticsPoint::recycledKg).reversed())
                .toList();

        List<MonthlyTrendPoint> monthlyTrends = monthMap.values().stream()
                .map(MonthlyAggregator::toPoint)
                .toList();

        return new ProcessorAnalyticsResponse(
                profile.getId(),
                profile.getCompanyName(),
                totalProcessedKg.setScale(2, RoundingMode.HALF_UP),
                receipts.size(),
                maxCap != null ? maxCap : BigDecimal.ZERO,
                currentLoad != null ? currentLoad : BigDecimal.ZERO,
                utilizationPercent,
                impact.totalCo2AvoidedKg(),
                impact.totalCo2AvoidedMt(),
                impact.totalLandfillDivertedKg(),
                categoryDistribution,
                monthlyTrends,
                EnvironmentalImpactCalculator.ESTIMATE_DISCLAIMER
        );
    }

    /**
     * Unified monthly collection, recycling, and carbon avoidance timeline.
     */
    public List<MonthlyTrendPoint> getUnifiedMonthlyTrends(Instant startDate, Instant endDate) {
        List<Transaction> transactions = transactionRepository.findCompletedTransactionsForAnalytics(startDate, endDate, null);
        List<ProcessorReceipt> receipts = processorReceiptRepository.findConfirmedReceiptsForAnalytics(startDate, endDate, null);
        Map<Long, EnvironmentalEmissionFactor> factorMap = loadEffectiveFactorMap();

        Map<String, MonthlyAggregator> monthMap = new TreeMap<>();

        for (Transaction t : transactions) {
            String month = formatMonth(t.getTransactionDate());
            monthMap.computeIfAbsent(month, MonthlyAggregator::new)
                    .addCollection(t.getQuantity() != null ? t.getQuantity() : BigDecimal.ZERO);
        }

        for (ProcessorReceipt r : receipts) {
            String month = formatMonth(r.getReceivedDate());
            BigDecimal qty = r.getQuantity() != null ? r.getQuantity() : BigDecimal.ZERO;
            EnvironmentalEmissionFactor factor = factorMap.get(r.getCategory().getId());

            BigDecimal co2 = BigDecimal.ZERO;
            BigDecimal landfill = qty;
            if (factor != null) {
                if (factor.getCo2FactorKgPerUnit() != null) {
                    co2 = qty.multiply(factor.getCo2FactorKgPerUnit()).setScale(2, RoundingMode.HALF_UP);
                }
                if (factor.getLandfillDiversionRate() != null) {
                    landfill = qty.multiply(factor.getLandfillDiversionRate()).setScale(2, RoundingMode.HALF_UP);
                }
            }

            monthMap.computeIfAbsent(month, MonthlyAggregator::new)
                    .addReceiptWithImpact(qty, co2, landfill);
        }

        return monthMap.values().stream()
                .map(MonthlyAggregator::toPoint)
                .toList();
    }

    /**
     * Unified category-wise collection, recycling, and environmental impact points.
     */
    public List<CategoryAnalyticsPoint> getUnifiedCategoryAnalytics(Instant startDate, Instant endDate) {
        List<Transaction> transactions = transactionRepository.findCompletedTransactionsForAnalytics(startDate, endDate, null);
        List<ProcessorReceipt> receipts = processorReceiptRepository.findConfirmedReceiptsForAnalytics(startDate, endDate, null);
        Map<Long, EnvironmentalEmissionFactor> factorMap = loadEffectiveFactorMap();

        Map<Long, CategoryAggregator> catMap = new LinkedHashMap<>();

        // Ensure all platform categories appear in the list
        List<Category> allCategories = categoryRepository.findAll();
        for (Category c : allCategories) {
            catMap.put(c.getId(), new CategoryAggregator(c.getId(), c.getName(), c.getSlug()));
        }

        for (Transaction t : transactions) {
            Category c = t.getListing().getCategory();
            catMap.computeIfAbsent(c.getId(), id -> new CategoryAggregator(c.getId(), c.getName(), c.getSlug()))
                    .addCollected(t.getQuantity() != null ? t.getQuantity() : BigDecimal.ZERO);
        }

        for (ProcessorReceipt r : receipts) {
            Category c = r.getCategory();
            catMap.computeIfAbsent(c.getId(), id -> new CategoryAggregator(c.getId(), c.getName(), c.getSlug()))
                    .addRecycled(r.getQuantity() != null ? r.getQuantity() : BigDecimal.ZERO);
        }

        return catMap.values().stream()
                .map(ca -> {
                    EnvironmentalEmissionFactor factor = factorMap.get(ca.categoryId);
                    BigDecimal co2Factor = factor != null ? factor.getCo2FactorKgPerUnit() : null;
                    boolean isConfigured = co2Factor != null;
                    BigDecimal diversionRate = factor != null && factor.getLandfillDiversionRate() != null
                            ? factor.getLandfillDiversionRate()
                            : BigDecimal.ONE;

                    BigDecimal co2Avoided = (isConfigured && ca.recycledKg.compareTo(BigDecimal.ZERO) > 0)
                            ? ca.recycledKg.multiply(co2Factor).setScale(2, RoundingMode.HALF_UP)
                            : (isConfigured ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : null);

                    BigDecimal landfillDiverted = ca.recycledKg.multiply(diversionRate).setScale(2, RoundingMode.HALF_UP);
                    String source = factor != null ? factor.getSourceReference() : "Unconfigured";

                    return new CategoryAnalyticsPoint(
                            ca.categoryId,
                            ca.categoryName,
                            ca.categorySlug,
                            ca.collectedKg.setScale(2, RoundingMode.HALF_UP),
                            ca.recycledKg.setScale(2, RoundingMode.HALF_UP),
                            co2Factor,
                            co2Avoided,
                            landfillDiverted,
                            isConfigured,
                            source
                    );
                })
                .sorted(Comparator.comparing(CategoryAnalyticsPoint::collectedKg)
                        .thenComparing(CategoryAnalyticsPoint::recycledKg).reversed())
                .toList();
    }

    /**
     * Lists all environmental emission factors with category details.
     */
    public List<EmissionFactorResponse> getAllEmissionFactors() {
        return factorRepository.findAllWithCategory().stream()
                .map(EmissionFactorResponse::from)
                .toList();
    }

    /**
     * Admin update of emission factor values and methodology documentation.
     */
    @Transactional
    public EmissionFactorResponse updateEmissionFactor(Long factorId, UpdateEmissionFactorRequest request) {
        EnvironmentalEmissionFactor factor = factorRepository.findById(factorId)
                .orElseThrow(() -> new ResourceNotFoundException("Emission factor not found with id: " + factorId));

        if (request.co2FactorKgPerUnit() != null) {
            factor.setCo2FactorKgPerUnit(request.co2FactorKgPerUnit());
        }
        if (request.landfillDiversionRate() != null) {
            factor.setLandfillDiversionRate(request.landfillDiversionRate());
        }
        if (request.sourceReference() != null && !request.sourceReference().isBlank()) {
            factor.setSourceReference(request.sourceReference());
        }
        if (request.methodologyNotes() != null) {
            factor.setMethodologyNotes(request.methodologyNotes());
        }
        if (request.isActive() != null) {
            factor.setActive(request.isActive());
        }

        EnvironmentalEmissionFactor saved = factorRepository.save(factor);
        return EmissionFactorResponse.from(saved);
    }

    private Map<Long, EnvironmentalEmissionFactor> loadEffectiveFactorMap() {
        List<EnvironmentalEmissionFactor> activeFactors = factorRepository.findAllActiveWithCategory();
        Map<Long, EnvironmentalEmissionFactor> map = new HashMap<>();
        for (EnvironmentalEmissionFactor f : activeFactors) {
            map.put(f.getCategory().getId(), f);
        }
        // Fallback: If a subcategory does not have its own factor, map to parent category factor if available
        List<Category> allCategories = categoryRepository.findAll();
        for (Category cat : allCategories) {
            if (!map.containsKey(cat.getId()) && cat.getParentCategory() != null && map.containsKey(cat.getParentCategory().getId())) {
                map.put(cat.getId(), map.get(cat.getParentCategory().getId()));
            }
        }
        return map;
    }

    private String formatMonth(Instant instant) {
        if (instant == null) return "Unknown";
        return LocalDate.ofInstant(instant, ZoneOffset.UTC).format(MONTH_FORMATTER);
    }

    private static class CategoryAggregator {
        final Long categoryId;
        final String categoryName;
        final String categorySlug;
        BigDecimal collectedKg = BigDecimal.ZERO;
        BigDecimal recycledKg = BigDecimal.ZERO;

        CategoryAggregator(Long categoryId, String categoryName, String categorySlug) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.categorySlug = categorySlug;
        }

        void addCollected(BigDecimal qty) {
            this.collectedKg = this.collectedKg.add(qty);
        }

        void addRecycled(BigDecimal qty) {
            this.recycledKg = this.recycledKg.add(qty);
        }
    }

    private static class ProcessorAggregator {
        final Long processorId;
        final String companyName;
        final String licenseNumber;
        BigDecimal recycledKg = BigDecimal.ZERO;
        long receiptsCount = 0;

        ProcessorAggregator(Long processorId, String companyName, String licenseNumber) {
            this.processorId = processorId;
            this.companyName = companyName;
            this.licenseNumber = licenseNumber;
        }

        void addReceipt(BigDecimal qty) {
            this.recycledKg = this.recycledKg.add(qty);
            this.receiptsCount++;
        }
    }

    private static class MonthlyAggregator {
        final String month;
        BigDecimal collectedKg = BigDecimal.ZERO;
        BigDecimal recycledKg = BigDecimal.ZERO;
        BigDecimal co2AvoidedKg = BigDecimal.ZERO;
        BigDecimal landfillDivertedKg = BigDecimal.ZERO;
        long transactionsCount = 0;
        long receiptsCount = 0;

        MonthlyAggregator(String month) {
            this.month = month;
        }

        void addCollection(BigDecimal qty) {
            this.collectedKg = this.collectedKg.add(qty);
            this.transactionsCount++;
        }

        void addReceipt(BigDecimal qty) {
            this.recycledKg = this.recycledKg.add(qty);
            this.receiptsCount++;
        }

        void addReceiptWithImpact(BigDecimal qty, BigDecimal co2, BigDecimal landfill) {
            this.recycledKg = this.recycledKg.add(qty);
            this.co2AvoidedKg = this.co2AvoidedKg.add(co2);
            this.landfillDivertedKg = this.landfillDivertedKg.add(landfill);
            this.receiptsCount++;
        }

        MonthlyTrendPoint toPoint() {
            return new MonthlyTrendPoint(
                    month,
                    collectedKg.setScale(2, RoundingMode.HALF_UP),
                    recycledKg.setScale(2, RoundingMode.HALF_UP),
                    co2AvoidedKg.setScale(2, RoundingMode.HALF_UP),
                    landfillDivertedKg.setScale(2, RoundingMode.HALF_UP),
                    transactionsCount,
                    receiptsCount
            );
        }
    }
}
