package com.waste2value.listing;

import com.waste2value.address.Address;
import com.waste2value.address.AddressService;
import com.waste2value.category.Category;
import com.waste2value.category.CategoryService;
import com.waste2value.listing.dto.*;
import com.waste2value.shared.PageResponse;
import com.waste2value.shared.exception.BadRequestException;
import com.waste2value.shared.exception.InvalidStateTransitionException;
import com.waste2value.shared.exception.ResourceNotFoundException;
import com.waste2value.storage.StorageService;
import com.waste2value.user.User;
import com.waste2value.user.UserService;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ListingService {

    private final ListingRepository listingRepository;
    private final ListingImageRepository listingImageRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final AddressService addressService;
    private final StorageService storageService;

    public ListingService(
            ListingRepository listingRepository,
            ListingImageRepository listingImageRepository,
            UserService userService,
            CategoryService categoryService,
            AddressService addressService,
            StorageService storageService) {
        this.listingRepository = listingRepository;
        this.listingImageRepository = listingImageRepository;
        this.userService = userService;
        this.categoryService = categoryService;
        this.addressService = addressService;
        this.storageService = storageService;
    }

    @Transactional
    public ListingResponse createListing(Long generatorId, CreateListingRequest request) {
        User generator = userService.getUserEntity(generatorId);
        Category category = categoryService.getCategoryEntity(request.categoryId());
        if (!category.isActive()) {
            throw new BadRequestException("Selected category is inactive");
        }

        Address address = addressService.getAddressEntity(request.addressId(), generatorId);

        BigDecimal price = request.expectedPrice() != null ? request.expectedPrice() : BigDecimal.ZERO;

        Listing listing = new Listing(
                generator,
                category,
                address,
                request.title().trim(),
                request.description().trim(),
                request.quantity(),
                request.unit().trim(),
                price,
                ListingStatus.DRAFT
        );

        Listing saved = listingRepository.save(listing);
        return ListingResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ListingResponse getListing(Long id, Long currentUserIdOrNull, boolean isAdmin) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        if (listing.getStatus() == ListingStatus.POSTED) {
            return ListingResponse.from(listing);
        }

        if (isAdmin || (currentUserIdOrNull != null && currentUserIdOrNull.equals(listing.getGenerator().getId()))) {
            return ListingResponse.from(listing);
        }

        throw new ResourceNotFoundException("Listing not found");
    }

    @Transactional
    public ListingResponse updateListing(Long id, Long generatorId, UpdateListingRequest request) {
        Listing listing = getOwnedListingEntity(id, generatorId);

        if (listing.getStatus() != ListingStatus.DRAFT) {
            throw new InvalidStateTransitionException("Only DRAFT listings can be updated. Current status: " + listing.getStatus());
        }

        Category category = categoryService.getCategoryEntity(request.categoryId());
        if (!category.isActive()) {
            throw new BadRequestException("Selected category is inactive");
        }

        Address address = addressService.getAddressEntity(request.addressId(), generatorId);

        listing.setCategory(category);
        listing.setAddress(address);
        listing.setTitle(request.title().trim());
        listing.setDescription(request.description().trim());
        listing.setQuantity(request.quantity());
        listing.setUnit(request.unit().trim());
        listing.setExpectedPrice(request.expectedPrice() != null ? request.expectedPrice() : BigDecimal.ZERO);

        Listing updated = listingRepository.save(listing);
        return ListingResponse.from(updated);
    }

    @Transactional
    public ListingResponse publishListing(Long id, Long generatorId) {
        Listing listing = getOwnedListingEntity(id, generatorId);

        if (listing.getStatus() != ListingStatus.DRAFT) {
            throw new InvalidStateTransitionException("Only DRAFT listings can be published. Current status: " + listing.getStatus());
        }

        listing.setStatus(ListingStatus.POSTED);
        Listing updated = listingRepository.save(listing);
        return ListingResponse.from(updated);
    }

    @Transactional
    public ListingResponse cancelListing(Long id, Long generatorId) {
        Listing listing = getOwnedListingEntity(id, generatorId);

        if (listing.getStatus() != ListingStatus.DRAFT && listing.getStatus() != ListingStatus.POSTED) {
            throw new InvalidStateTransitionException("Cannot cancel listing with status: " + listing.getStatus() + ". Only DRAFT or POSTED listings can be cancelled.");
        }

        listing.setStatus(ListingStatus.CANCELLED);
        Listing updated = listingRepository.save(listing);
        return ListingResponse.from(updated);
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingSummaryResponse> getMyListings(Long generatorId, ListingStatus status, Pageable pageable) {
        Page<Listing> page = (status != null)
                ? listingRepository.findByGeneratorIdAndStatus(generatorId, status, pageable)
                : listingRepository.findByGeneratorId(generatorId, pageable);

        return PageResponse.from(page.map(ListingSummaryResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingSummaryResponse> searchListings(ListingSearchCriteria criteria, Pageable pageable) {
        // Enforce POSTED status for public marketplace search unless explicitly specified by admin
        ListingStatus searchStatus = criteria.status() != null ? criteria.status() : ListingStatus.POSTED;
        ListingSearchCriteria effectiveCriteria = new ListingSearchCriteria(
                criteria.keyword(),
                criteria.categoryId(),
                criteria.city(),
                criteria.state(),
                criteria.minQuantity(),
                criteria.maxQuantity(),
                criteria.minPrice(),
                criteria.maxPrice(),
                searchStatus,
                criteria.latitude(),
                criteria.longitude(),
                criteria.maxDistanceKm(),
                criteria.sortBy()
        );

        Specification<Listing> spec = ListingSpecification.build(effectiveCriteria);

        // Sorting determination
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("price_asc".equalsIgnoreCase(criteria.sortBy())) {
            sort = Sort.by(Sort.Direction.ASC, "expectedPrice");
        } else if ("price_desc".equalsIgnoreCase(criteria.sortBy())) {
            sort = Sort.by(Sort.Direction.DESC, "expectedPrice");
        } else if ("quantity_asc".equalsIgnoreCase(criteria.sortBy())) {
            sort = Sort.by(Sort.Direction.ASC, "quantity");
        } else if ("quantity_desc".equalsIgnoreCase(criteria.sortBy())) {
            sort = Sort.by(Sort.Direction.DESC, "quantity");
        }

        Pageable effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Page<Listing> rawPage = listingRepository.findAll(spec, effectivePageable);

        boolean hasCoordinates = criteria.latitude() != null && criteria.longitude() != null;
        if (hasCoordinates) {
            List<ListingSummaryResponse> items = new ArrayList<>();
            for (Listing listing : rawPage.getContent()) {
                double dist = calculateDistanceKm(
                        criteria.latitude(), criteria.longitude(),
                        listing.getAddress().getLatitude().doubleValue(),
                        listing.getAddress().getLongitude().doubleValue()
                );

                if (criteria.maxDistanceKm() == null || dist <= criteria.maxDistanceKm()) {
                    items.add(ListingSummaryResponse.from(listing, Math.round(dist * 10.0) / 10.0));
                }
            }

            if ("distance_asc".equalsIgnoreCase(criteria.sortBy())) {
                items.sort(Comparator.comparing(ListingSummaryResponse::distanceKm, Comparator.nullsLast(Comparator.naturalOrder())));
            }

            Page<ListingSummaryResponse> customPage = new PageImpl<>(items, effectivePageable, rawPage.getTotalElements());
            return PageResponse.from(customPage);
        }

        return PageResponse.from(rawPage.map(ListingSummaryResponse::from));
    }

    @Transactional
    public ListingImageResponse addImageToListing(Long listingId, Long generatorId, MultipartFile file) {
        Listing listing = getOwnedListingEntity(listingId, generatorId);

        if (listing.getStatus() != ListingStatus.DRAFT && listing.getStatus() != ListingStatus.POSTED) {
            throw new InvalidStateTransitionException("Cannot add images to listing in status: " + listing.getStatus());
        }

        if (listing.getImages().size() >= 5) {
            throw new BadRequestException("Maximum 5 images allowed per listing");
        }

        String storedPath = storageService.store(file, "listings/" + listingId);

        ListingImage image = new ListingImage(
                listing,
                storedPath,
                storedPath,
                file.getSize(),
                file.getContentType()
        );

        listing.addImage(image);
        ListingImage savedImage = listingImageRepository.save(image);
        return ListingImageResponse.from(savedImage);
    }

    @Transactional
    public void removeImageFromListing(Long listingId, Long imageId, Long generatorId) {
        Listing listing = getOwnedListingEntity(listingId, generatorId);

        if (listing.getStatus() != ListingStatus.DRAFT) {
            throw new InvalidStateTransitionException("Can only remove images from DRAFT listings");
        }

        ListingImage image = listingImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        if (!image.getListing().getId().equals(listingId)) {
            throw new BadRequestException("Image does not belong to this listing");
        }

        listing.removeImage(image);
        storageService.delete(image.getFileName());
        listingImageRepository.delete(image);
    }

    @Transactional(readOnly = true)
    public Listing getOwnedListingEntity(Long id, Long generatorId) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        if (!listing.getGenerator().getId().equals(generatorId)) {
            // Prevent cross-user access leaking existence
            throw new ResourceNotFoundException("Listing not found");
        }

        return listing;
    }

    // Haversine formula to compute distance in km between two GPS coordinates
    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
