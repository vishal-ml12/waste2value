package com.waste2value.processor;

import com.waste2value.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "processor_profiles")
public class ProcessorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "license_number", nullable = false, length = 100)
    private String licenseNumber;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "max_capacity_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal maxCapacityKg = BigDecimal.ZERO;

    @Column(name = "current_load_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentLoadKg = BigDecimal.ZERO;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Column(name = "avg_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private User verifiedBy;

    @OneToMany(mappedBy = "processor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProcessorCapability> capabilities = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public ProcessorProfile() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getMaxCapacityKg() {
        return maxCapacityKg;
    }

    public void setMaxCapacityKg(BigDecimal maxCapacityKg) {
        this.maxCapacityKg = maxCapacityKg;
    }

    public BigDecimal getCurrentLoadKg() {
        return currentLoadKg;
    }

    public void setCurrentLoadKg(BigDecimal currentLoadKg) {
        this.currentLoadKg = currentLoadKg;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(BigDecimal avgRating) {
        this.avgRating = avgRating;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public User getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(User verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public List<ProcessorCapability> getCapabilities() {
        return capabilities;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
