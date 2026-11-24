package com.waste2value.pickup;

import com.waste2value.listing.Listing;
import com.waste2value.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pickup_requests")
public class PickupRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collector_id", nullable = false)
    private User collector;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PickupRequestStatus status = PickupRequestStatus.REQUESTED;

    @Column(name = "proposed_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal proposedPrice;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "actual_quantity", precision = 12, scale = 2)
    private BigDecimal actualQuantity;

    @Column(name = "completion_note", columnDefinition = "TEXT")
    private String completionNote;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt = Instant.now();

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.requestedAt == null) {
            this.requestedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public PickupRequest() {}

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

    public User getCollector() {
        return collector;
    }

    public void setCollector(User collector) {
        this.collector = collector;
    }

    public PickupRequestStatus getStatus() {
        return status;
    }

    public void setStatus(PickupRequestStatus status) {
        this.status = status;
    }

    public BigDecimal getProposedPrice() {
        return proposedPrice;
    }

    public void setProposedPrice(BigDecimal proposedPrice) {
        this.proposedPrice = proposedPrice;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public BigDecimal getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(BigDecimal actualQuantity) {
        this.actualQuantity = actualQuantity;
    }

    public String getCompletionNote() {
        return completionNote;
    }

    public void setCompletionNote(String completionNote) {
        this.completionNote = completionNote;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
