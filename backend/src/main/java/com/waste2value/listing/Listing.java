package com.waste2value.listing;

import com.waste2value.address.Address;
import com.waste2value.category.Category;
import com.waste2value.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "listings")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generator_id", nullable = false)
    private User generator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "expected_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal expectedPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ListingStatus status = ListingStatus.DRAFT;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ListingImage> images = new ArrayList<>();

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Listing() {
    }

    public Listing(User generator, Category category, Address address, String title, String description,
                   BigDecimal quantity, String unit, BigDecimal expectedPrice, ListingStatus status) {
        this.generator = generator;
        this.category = category;
        this.address = address;
        this.title = title;
        this.description = description;
        this.quantity = quantity;
        this.unit = unit;
        this.expectedPrice = expectedPrice != null ? expectedPrice : BigDecimal.ZERO;
        this.status = status != null ? status : ListingStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addImage(ListingImage image) {
        images.add(image);
        image.setListing(this);
    }

    public void removeImage(ListingImage image) {
        images.remove(image);
        image.setListing(null);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getGenerator() {
        return generator;
    }

    public void setGenerator(User generator) {
        this.generator = generator;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public BigDecimal getExpectedPrice() {
        return expectedPrice;
    }

    public void setExpectedPrice(BigDecimal expectedPrice) {
        this.expectedPrice = expectedPrice;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public void setStatus(ListingStatus status) {
        this.status = status;
    }

    public List<ListingImage> getImages() {
        return images;
    }

    public void setImages(List<ListingImage> images) {
        this.images = images;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
