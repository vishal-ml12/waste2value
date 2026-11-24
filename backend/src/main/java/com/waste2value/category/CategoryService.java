package com.waste2value.category;

import com.waste2value.category.dto.CategoryRequest;
import com.waste2value.category.dto.CategoryResponse;
import com.waste2value.category.dto.CategoryTreeResponse;
import com.waste2value.shared.exception.ConflictException;
import com.waste2value.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getCategoryTree(boolean activeOnly) {
        List<Category> roots = activeOnly
                ? categoryRepository.findByParentCategoryIsNullAndActiveTrueOrderByNameAsc()
                : categoryRepository.findByParentCategoryIsNullOrderByNameAsc();

        return roots.stream()
                .map(root -> CategoryTreeResponse.from(root, activeOnly))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategoriesFlat(boolean activeOnly) {
        List<Category> categories = activeOnly
                ? categoryRepository.findAllActive()
                : categoryRepository.findAll();

        return categories.stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = getCategoryEntity(id);
        return CategoryResponse.from(category);
    }

    @Transactional(readOnly = true)
    public Category getCategoryEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        Category parent = null;
        if (request.parentId() != null) {
            parent = getCategoryEntity(request.parentId());
            if (categoryRepository.existsByNameIgnoreCaseAndParentCategoryId(request.name().trim(), request.parentId())) {
                throw new ConflictException("Category with name '" + request.name() + "' already exists under parent '" + parent.getName() + "'");
            }
        } else {
            if (categoryRepository.existsByNameIgnoreCaseAndParentCategoryIsNull(request.name().trim())) {
                throw new ConflictException("Root category with name '" + request.name() + "' already exists");
            }
        }

        String slug = request.slug() != null && !request.slug().isBlank()
                ? toSlug(request.slug())
                : toSlug(request.name());

        if (categoryRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis() % 10000;
        }

        boolean active = request.active() == null || request.active();
        Category category = new Category(
                parent,
                request.name().trim(),
                slug,
                request.description() != null ? request.description().trim() : null,
                request.unit() != null ? request.unit().trim() : "kg",
                active
        );

        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = getCategoryEntity(id);

        Category parent = null;
        if (request.parentId() != null) {
            if (request.parentId().equals(id)) {
                throw new ConflictException("A category cannot be its own parent");
            }
            parent = getCategoryEntity(request.parentId());
            if (categoryRepository.existsByNameIgnoreCaseAndParentCategoryIdAndIdNot(request.name().trim(), request.parentId(), id)) {
                throw new ConflictException("Category with name '" + request.name() + "' already exists under parent '" + parent.getName() + "'");
            }
        } else {
            if (categoryRepository.existsByNameIgnoreCaseAndParentCategoryIsNullAndIdNot(request.name().trim(), id)) {
                throw new ConflictException("Root category with name '" + request.name() + "' already exists");
            }
        }

        String slug = request.slug() != null && !request.slug().isBlank()
                ? toSlug(request.slug())
                : toSlug(request.name());

        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            slug = slug + "-" + id;
        }

        category.setParentCategory(parent);
        category.setName(request.name().trim());
        category.setSlug(slug);
        category.setDescription(request.description() != null ? request.description().trim() : null);
        category.setUnit(request.unit() != null ? request.unit().trim() : "kg");
        if (request.active() != null) {
            category.setActive(request.active());
        }

        Category updated = categoryRepository.save(category);
        return CategoryResponse.from(updated);
    }

    @Transactional
    public CategoryResponse setCategoryStatus(Long id, boolean active) {
        Category category = getCategoryEntity(id);
        category.setActive(active);
        Category updated = categoryRepository.save(category);
        return CategoryResponse.from(updated);
    }

    private String toSlug(String input) {
        return input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
