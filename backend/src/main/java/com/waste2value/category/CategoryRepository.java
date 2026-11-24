package com.waste2value.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentCategoryIsNullOrderByNameAsc();

    List<Category> findByParentCategoryIsNullAndActiveTrueOrderByNameAsc();

    List<Category> findByParentCategoryIdOrderByNameAsc(Long parentId);

    List<Category> findByParentCategoryIdAndActiveTrueOrderByNameAsc(Long parentId);

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByNameIgnoreCaseAndParentCategoryId(String name, Long parentId);

    boolean existsByNameIgnoreCaseAndParentCategoryIsNull(String name);

    boolean existsByNameIgnoreCaseAndParentCategoryIdAndIdNot(String name, Long parentId, Long id);

    boolean existsByNameIgnoreCaseAndParentCategoryIsNullAndIdNot(String name, Long id);

    @Query("SELECT c FROM Category c WHERE c.active = true ORDER BY c.name ASC")
    List<Category> findAllActive();
}
