package com.waste2value.listing;

import com.waste2value.address.Address;
import com.waste2value.category.Category;
import com.waste2value.listing.dto.ListingSearchCriteria;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ListingSpecification {

    public static Specification<Listing> build(ListingSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Status filter
            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }

            // Keyword filter (title, description, or category name)
            if (StringUtils.hasText(criteria.keyword())) {
                String pattern = "%" + criteria.keyword().trim().toLowerCase() + "%";
                Join<Listing, Category> categoryJoin = root.join("category", JoinType.LEFT);

                Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
                Predicate catLike = cb.like(cb.lower(categoryJoin.get("name")), pattern);

                predicates.add(cb.or(titleLike, descLike, catLike));
            }

            // Category filter: match category itself or parent category
            if (criteria.categoryId() != null) {
                Join<Listing, Category> catJoin = root.join("category", JoinType.INNER);
                Join<Category, Category> parentCatJoin = catJoin.join("parentCategory", JoinType.LEFT);

                Predicate directCat = cb.equal(catJoin.get("id"), criteria.categoryId());
                Predicate parentCat = cb.equal(parentCatJoin.get("id"), criteria.categoryId());
                predicates.add(cb.or(directCat, parentCat));
            }

            // Location filters (city & state)
            if (StringUtils.hasText(criteria.city())) {
                Join<Listing, Address> addressJoin = root.join("address", JoinType.INNER);
                predicates.add(cb.like(cb.lower(addressJoin.get("city")), "%" + criteria.city().trim().toLowerCase() + "%"));
            }

            if (StringUtils.hasText(criteria.state())) {
                Join<Listing, Address> addressJoin = root.join("address", JoinType.INNER);
                predicates.add(cb.like(cb.lower(addressJoin.get("state")), "%" + criteria.state().trim().toLowerCase() + "%"));
            }

            // Quantity range
            if (criteria.minQuantity() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("quantity"), criteria.minQuantity()));
            }
            if (criteria.maxQuantity() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("quantity"), criteria.maxQuantity()));
            }

            // Price range
            if (criteria.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("expectedPrice"), criteria.minPrice()));
            }
            if (criteria.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("expectedPrice"), criteria.maxPrice()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
