package com.waste2value.transaction;

import com.waste2value.listing.Listing;
import com.waste2value.pickup.PickupRequest;
import com.waste2value.processor.ProcessorProfile;
import com.waste2value.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pickup_request_id", nullable = false, unique = true)
    private PickupRequest pickupRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generator_id", nullable = false)
    private User generator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collector_id", nullable = false)
    private User collector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_id")
    private ProcessorProfile processor;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "agreed_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal agreedPrice;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionStatus status = TransactionStatus.COMPLETED;

    @Column(name = "transaction_date", nullable = false)
    private Instant transactionDate = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.transactionDate == null) {
            this.transactionDate = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Transaction() {}

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

    public User getGenerator() {
        return generator;
    }

    public void setGenerator(User generator) {
        this.generator = generator;
    }

    public User getCollector() {
        return collector;
    }

    public void setCollector(User collector) {
        this.collector = collector;
    }

    public ProcessorProfile getProcessor() {
        return processor;
    }

    public void setProcessor(ProcessorProfile processor) {
        this.processor = processor;
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

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public Instant getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Instant transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
