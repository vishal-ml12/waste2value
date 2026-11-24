package com.waste2value.receipt;

import com.waste2value.category.Category;
import com.waste2value.listing.Listing;
import com.waste2value.pickup.PickupRequest;
import com.waste2value.processor.ProcessorProfile;
import com.waste2value.transaction.Transaction;
import com.waste2value.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "processor_receipts")
public class ProcessorReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_request_id")
    private PickupRequest pickupRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processor_id", nullable = false)
    private ProcessorProfile processor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collector_id", nullable = false)
    private User collector;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "agreed_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal agreedPrice = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProcessorReceiptStatus status = ProcessorReceiptStatus.RECEIVED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "received_date", nullable = false)
    private Instant receivedDate = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.receivedDate == null) {
            this.receivedDate = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public ProcessorReceipt() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Listing getListing() {
        return listing;
    }

    public void setListing(Listing listing) {
        this.listing = listing;
    }

    public PickupRequest getPickupRequest() {
        return pickupRequest;
    }

    public void setPickupRequest(PickupRequest pickupRequest) {
        this.pickupRequest = pickupRequest;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public ProcessorProfile getProcessor() {
        return processor;
    }

    public void setProcessor(ProcessorProfile processor) {
        this.processor = processor;
    }

    public User getCollector() {
        return collector;
    }

    public void setCollector(User collector) {
        this.collector = collector;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getAgreedPrice() {
        return agreedPrice;
    }

    public void setAgreedPrice(BigDecimal agreedPrice) {
        this.agreedPrice = agreedPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public ProcessorReceiptStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessorReceiptStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(Instant receivedDate) {
        this.receivedDate = receivedDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
