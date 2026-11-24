package com.waste2value.listing;

import com.waste2value.address.Address;
import com.waste2value.address.AddressService;
import com.waste2value.category.Category;
import com.waste2value.category.CategoryService;
import com.waste2value.listing.dto.*;
import com.waste2value.shared.PageResponse;
import com.waste2value.shared.exception.InvalidStateTransitionException;
import com.waste2value.shared.exception.ResourceNotFoundException;
import com.waste2value.storage.StorageService;
import com.waste2value.user.Role;
import com.waste2value.user.User;
import com.waste2value.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ListingImageRepository listingImageRepository;

    @Mock
    private UserService userService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private AddressService addressService;

    @Mock
    private StorageService storageService;

    private ListingService listingService;

    private User generator;
    private Category category;
    private Address address;

    @BeforeEach
    void setUp() {
        listingService = new ListingService(
                listingRepository, listingImageRepository, userService,
                categoryService, addressService, storageService
        );

        generator = new User("Eco", "Gen", "eco@example.com", "hash", "123", Role.GENERATOR);
        generator.setId(10L);

        category = new Category(null, "Plastic", "plastic", "Polymer", "kg", true);
        category.setId(1L);

        address = new Address(generator, "Line 1", null, "Mumbai", "MH", "400001",
                new BigDecimal("19.0760"), new BigDecimal("72.8777"), true);
        address.setId(100L);
    }

    @Test
    void createListing_InitialStatusIsDraft() {
        CreateListingRequest request = new CreateListingRequest(
                1L, 100L, "Scrap Plastic", "Sorted bottles",
                new BigDecimal("50.00"), "kg", new BigDecimal("15.00")
        );

        when(userService.getUserEntity(10L)).thenReturn(generator);
        when(categoryService.getCategoryEntity(1L)).thenReturn(category);
        when(addressService.getAddressEntity(100L, 10L)).thenReturn(address);

        Listing saved = new Listing(generator, category, address, request.title(), request.description(),
                request.quantity(), request.unit(), request.expectedPrice(), ListingStatus.DRAFT);
        saved.setId(500L);
        when(listingRepository.save(any(Listing.class))).thenReturn(saved);

        ListingResponse response = listingService.createListing(10L, request);

        assertNotNull(response);
        assertEquals(ListingStatus.DRAFT, response.status());
        assertEquals("Scrap Plastic", response.title());
    }

    @Test
    void updateListing_DraftStatus_Success() {
        Listing listing = new Listing(generator, category, address, "Old Title", "Old Desc",
                new BigDecimal("20.00"), "kg", BigDecimal.TEN, ListingStatus.DRAFT);
        listing.setId(501L);

        UpdateListingRequest updateReq = new UpdateListingRequest(
                1L, 100L, "New Title", "New Desc",
                new BigDecimal("30.00"), "kg", new BigDecimal("20.00")
        );

        when(listingRepository.findById(501L)).thenReturn(Optional.of(listing));
        when(categoryService.getCategoryEntity(1L)).thenReturn(category);
        when(addressService.getAddressEntity(100L, 10L)).thenReturn(address);
        when(listingRepository.save(any(Listing.class))).thenAnswer(i -> i.getArgument(0));

        ListingResponse response = listingService.updateListing(501L, 10L, updateReq);

        assertEquals("New Title", response.title());
        assertEquals(new BigDecimal("30.00"), response.quantity());
    }

    @Test
    void updateListing_NonDraft_ThrowsInvalidStateTransitionException() {
        Listing postedListing = new Listing(generator, category, address, "Posted Title", "Desc",
                new BigDecimal("20.00"), "kg", BigDecimal.TEN, ListingStatus.POSTED);
        postedListing.setId(502L);

        UpdateListingRequest updateReq = new UpdateListingRequest(
                1L, 100L, "Attempted Update", "Desc",
                new BigDecimal("30.00"), "kg", new BigDecimal("20.00")
        );

        when(listingRepository.findById(502L)).thenReturn(Optional.of(postedListing));

        assertThrows(InvalidStateTransitionException.class,
                () -> listingService.updateListing(502L, 10L, updateReq));
    }

    @Test
    void publishListing_DraftToPosted_Success() {
        Listing listing = new Listing(generator, category, address, "Draft Title", "Desc",
                new BigDecimal("20.00"), "kg", BigDecimal.TEN, ListingStatus.DRAFT);
        listing.setId(503L);

        when(listingRepository.findById(503L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(i -> i.getArgument(0));

        ListingResponse response = listingService.publishListing(503L, 10L);

        assertEquals(ListingStatus.POSTED, response.status());
    }

    @Test
    void publishListing_AlreadyPosted_ThrowsInvalidStateTransitionException() {
        Listing listing = new Listing(generator, category, address, "Posted Title", "Desc",
                new BigDecimal("20.00"), "kg", BigDecimal.TEN, ListingStatus.POSTED);
        listing.setId(504L);

        when(listingRepository.findById(504L)).thenReturn(Optional.of(listing));

        assertThrows(InvalidStateTransitionException.class,
                () -> listingService.publishListing(504L, 10L));
    }

    @Test
    void cancelListing_DraftOrPosted_TransitionsToCancelled() {
        Listing listing = new Listing(generator, category, address, "Title", "Desc",
                new BigDecimal("20.00"), "kg", BigDecimal.TEN, ListingStatus.POSTED);
        listing.setId(505L);

        when(listingRepository.findById(505L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(i -> i.getArgument(0));

        ListingResponse response = listingService.cancelListing(505L, 10L);

        assertEquals(ListingStatus.CANCELLED, response.status());
    }

    @Test
    void cancelListing_CompletedListing_ThrowsInvalidStateTransitionException() {
        Listing listing = new Listing(generator, category, address, "Title", "Desc",
                new BigDecimal("20.00"), "kg", BigDecimal.TEN, ListingStatus.COMPLETED);
        listing.setId(506L);

        when(listingRepository.findById(506L)).thenReturn(Optional.of(listing));

        assertThrows(InvalidStateTransitionException.class,
                () -> listingService.cancelListing(506L, 10L));
    }

    @Test
    void getOwnedListing_DifferentUser_ThrowsResourceNotFoundException() {
        Listing listing = new Listing(generator, category, address, "Title", "Desc",
                new BigDecimal("20.00"), "kg", BigDecimal.TEN, ListingStatus.DRAFT);
        listing.setId(507L);

        when(listingRepository.findById(507L)).thenReturn(Optional.of(listing));

        // Attempted access by user 999 instead of 10
        assertThrows(ResourceNotFoundException.class,
                () -> listingService.getOwnedListingEntity(507L, 999L));
    }

    @Test
    void searchListings_CoordinatesFilterDistanceAccurately() {
        // Mumbai coordinate: 19.0760, 72.8777
        Listing listingNear = new Listing(generator, category, address, "Near Mumbai", "Desc",
                new BigDecimal("10.00"), "kg", BigDecimal.TEN, ListingStatus.POSTED);
        listingNear.setId(1L);

        // Address in Pune: 18.5204, 73.8567 (~120 km from Mumbai)
        Address puneAddress = new Address(generator, "Pune Line", null, "Pune", "MH", "411001",
                new BigDecimal("18.5204"), new BigDecimal("73.8567"), false);
        Listing listingFar = new Listing(generator, category, puneAddress, "Far Pune", "Desc",
                new BigDecimal("10.00"), "kg", BigDecimal.TEN, ListingStatus.POSTED);
        listingFar.setId(2L);

        ListingSearchCriteria criteria = new ListingSearchCriteria(
                null, null, null, null, null, null, null, null, ListingStatus.POSTED,
                19.0760, 72.8777, 50.0, "distance_asc" // Max 50 km distance!
        );

        when(listingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(listingNear, listingFar)));

        PageResponse<ListingSummaryResponse> response = listingService.searchListings(criteria, PageRequest.of(0, 10));

        // ListingFar is ~120km away, should be filtered out by maxDistanceKm = 50.0
        assertEquals(1, response.content().size());
        assertEquals("Near Mumbai", response.content().get(0).title());
        assertNotNull(response.content().get(0).distanceKm());
        assertTrue(response.content().get(0).distanceKm() < 5.0);
    }
}
