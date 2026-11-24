package com.waste2value.receipt;

import com.waste2value.category.Category;
import com.waste2value.category.CategoryRepository;
import com.waste2value.listing.Listing;
import com.waste2value.listing.ListingRepository;
import com.waste2value.notification.NotificationService;
import com.waste2value.notification.NotificationType;
import com.waste2value.pickup.PickupRequest;
import com.waste2value.pickup.PickupRequestRepository;
import com.waste2value.pickup.PickupRequestStatus;
import com.waste2value.processor.ProcessorCapability;
import com.waste2value.processor.ProcessorCapabilityRepository;
import com.waste2value.processor.ProcessorProfile;
import com.waste2value.processor.ProcessorProfileRepository;
import com.waste2value.receipt.dto.CreateProcessorReceiptRequest;
import com.waste2value.receipt.dto.ProcessorReceiptResponse;
import com.waste2value.shared.PageResponse;
import com.waste2value.shared.exception.BadRequestException;
import com.waste2value.shared.exception.ForbiddenException;
import com.waste2value.shared.exception.ResourceNotFoundException;
import com.waste2value.transaction.Transaction;
import com.waste2value.transaction.TransactionRepository;
import com.waste2value.user.Role;
import com.waste2value.user.User;
import com.waste2value.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;

@Service
public class ProcessorReceiptService {

    private final ProcessorReceiptRepository processorReceiptRepository;
    private final ProcessorProfileRepository processorProfileRepository;
    private final ProcessorCapabilityRepository processorCapabilityRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ListingRepository listingRepository;
    private final PickupRequestRepository pickupRequestRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    public ProcessorReceiptService(
            ProcessorReceiptRepository processorReceiptRepository,
            ProcessorProfileRepository processorProfileRepository,
            ProcessorCapabilityRepository processorCapabilityRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            ListingRepository listingRepository,
            PickupRequestRepository pickupRequestRepository,
            TransactionRepository transactionRepository,
            NotificationService notificationService
    ) {
        this.processorReceiptRepository = processorReceiptRepository;
        this.processorProfileRepository = processorProfileRepository;
        this.processorCapabilityRepository = processorCapabilityRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.listingRepository = listingRepository;
        this.pickupRequestRepository = pickupRequestRepository;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ProcessorReceiptResponse recordReceipt(Long processorUserId, CreateProcessorReceiptRequest request) {
        // Concurrency Guard: acquire pessimistic write lock on the processor facility profile
        ProcessorProfile processor = processorProfileRepository.findByUserIdWithLock(processorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Processor profile not found for current user"));

        if (!processor.isVerified()) {
            throw new ForbiddenException("Your processor facility profile is pending admin verification. Only verified processors can record material receipts.");
        }

        User collector = userRepository.findById(request.collectorId())
                .orElseThrow(() -> new ResourceNotFoundException("Collector not found with id: " + request.collectorId()));

        if (collector.getRole() != Role.COLLECTOR) {
            throw new BadRequestException("Specified delivery source user must have role COLLECTOR");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

        // Traceability Validation & Relationship Linking
        Listing listing = null;
        PickupRequest pickupRequest = null;
        Transaction transaction = null;

        if (request.pickupRequestId() != null) {
            pickupRequest = pickupRequestRepository.findById(request.pickupRequestId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pickup request not found with id: " + request.pickupRequestId()));

            if (pickupRequest.getStatus() != PickupRequestStatus.COMPLETED) {
                throw new BadRequestException("Processor receipt can only be linked to a COMPLETED pickup request. Current status: " + pickupRequest.getStatus());
            }

            if (!pickupRequest.getCollector().getId().equals(collector.getId())) {
                throw new BadRequestException("Specified collector (id: " + collector.getId() + ") does not match the collector who fulfilled pickup request #" + pickupRequest.getId());
            }

            if (!pickupRequest.getListing().getCategory().getId().equals(category.getId())) {
                throw new BadRequestException("Receipt category ('" + category.getName() + "') does not match pickup request category ('" + pickupRequest.getListing().getCategory().getName() + "')");
            }

            if (request.listingId() != null && !pickupRequest.getListing().getId().equals(request.listingId())) {
                throw new BadRequestException("Specified listing id (" + request.listingId() + ") does not match pickup request's originating listing id (" + pickupRequest.getListing().getId() + ")");
            }

            listing = pickupRequest.getListing();

            // Link transaction and establish processor relationship
            Optional<Transaction> txOpt = transactionRepository.findByPickupRequestId(pickupRequest.getId());
            if (txOpt.isPresent()) {
                transaction = txOpt.get();
                if (transaction.getProcessor() == null) {
                    transaction.setProcessor(processor);
                    transactionRepository.save(transaction);
                }
            }
        } else if (request.listingId() != null) {
            listing = listingRepository.findById(request.listingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + request.listingId()));

            if (!listing.getCategory().getId().equals(category.getId())) {
                throw new BadRequestException("Receipt category ('" + category.getName() + "') does not match listing category ('" + listing.getCategory().getName() + "')");
            }
        }

        // Check processor capability for this category
        Optional<ProcessorCapability> capOpt = processorCapabilityRepository.findByProcessorIdAndCategoryId(processor.getId(), category.getId());
        if (capOpt.isPresent()) {
            ProcessorCapability cap = capOpt.get();
            if (!cap.isActive()) {
                throw new BadRequestException("Your capability for category '" + category.getName() + "' is currently marked inactive");
            }
            if (cap.getMinQuantity().compareTo(BigDecimal.ZERO) > 0 && request.quantity().compareTo(cap.getMinQuantity()) < 0) {
                throw new BadRequestException("Quantity (" + request.quantity() + ") is below your declared minimum capability of " + cap.getMinQuantity() + " " + category.getUnit());
            }
            if (cap.getMaxQuantity().compareTo(BigDecimal.ZERO) > 0 && request.quantity().compareTo(cap.getMaxQuantity()) > 0) {
                throw new BadRequestException("Quantity (" + request.quantity() + ") exceeds your declared maximum capability of " + cap.getMaxQuantity() + " " + category.getUnit());
            }
        }

        // Capacity constraint validation under lock
        BigDecimal addedKg = request.quantity();
        BigDecimal newLoad = processor.getCurrentLoadKg().add(addedKg);
        if (processor.getMaxCapacityKg().compareTo(BigDecimal.ZERO) > 0 && newLoad.compareTo(processor.getMaxCapacityKg()) > 0) {
            BigDecimal available = processor.getMaxCapacityKg().subtract(processor.getCurrentLoadKg());
            if (available.compareTo(BigDecimal.ZERO) < 0) available = BigDecimal.ZERO;
            throw new BadRequestException("Exceeds facility capacity. Available capacity is " + available + " kg, but receipt attempts to add " + addedKg + " kg.");
        }

        // Update processor load transactionally
        processor.setCurrentLoadKg(newLoad);
        processorProfileRepository.save(processor);

        BigDecimal totalAmount = request.quantity().multiply(request.agreedPrice()).setScale(2, RoundingMode.HALF_UP);

        ProcessorReceipt receipt = new ProcessorReceipt();
        receipt.setProcessor(processor);
        receipt.setCollector(collector);
        receipt.setCategory(category);
        receipt.setListing(listing);
        receipt.setPickupRequest(pickupRequest);
        receipt.setTransaction(transaction);
        receipt.setQuantity(request.quantity());
        receipt.setUnit(request.unit());
        receipt.setAgreedPrice(request.agreedPrice());
        receipt.setTotalAmount(totalAmount);
        receipt.setStatus(ProcessorReceiptStatus.RECEIVED);
        receipt.setNotes(request.notes());
        receipt.setReceivedDate(Instant.now());

        receipt = processorReceiptRepository.save(receipt);

        // Notify collector
        notificationService.sendNotification(
                collector,
                NotificationType.PROCESSOR_RECEIPT,
                "Material Receipt Recorded 🏭",
                "Processor " + processor.getCompanyName() + " recorded delivery receipt of " + request.quantity() + " " + request.unit() + " of " + category.getName() + " (Total: ₹" + totalAmount + ").",
                "PROCESSOR_RECEIPT",
                receipt.getId()
        );

        return ProcessorReceiptResponse.from(receipt);
    }

    @Transactional(readOnly = true)
    public ProcessorReceiptResponse getReceiptById(Long userId, Role role, Long receiptId) {
        ProcessorReceipt receipt = processorReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Processor receipt not found with id: " + receiptId));

        boolean isAuthorized = role == Role.ADMIN
                || (receipt.getProcessor() != null && receipt.getProcessor().getUser().getId().equals(userId))
                || (receipt.getCollector() != null && receipt.getCollector().getId().equals(userId))
                || (receipt.getListing() != null && receipt.getListing().getGenerator() != null && receipt.getListing().getGenerator().getId().equals(userId))
                || (receipt.getTransaction() != null && receipt.getTransaction().getGenerator() != null && receipt.getTransaction().getGenerator().getId().equals(userId));

        if (!isAuthorized) {
            throw new ForbiddenException("You are not authorized to view this processor receipt");
        }

        return ProcessorReceiptResponse.from(receipt);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProcessorReceiptResponse> getMyReceipts(Long processorUserId, Pageable pageable) {
        ProcessorProfile processor = processorProfileRepository.findByUserId(processorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Processor profile not found for current user"));

        return PageResponse.from(processorReceiptRepository.findByProcessorId(processor.getId(), pageable).map(ProcessorReceiptResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProcessorReceiptResponse> getCollectorDeliveries(Long collectorUserId, Pageable pageable) {
        return PageResponse.from(processorReceiptRepository.findByCollectorId(collectorUserId, pageable).map(ProcessorReceiptResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProcessorReceiptResponse> getAllReceipts(Pageable pageable) {
        return PageResponse.from(processorReceiptRepository.findAll(pageable).map(ProcessorReceiptResponse::from));
    }
}
