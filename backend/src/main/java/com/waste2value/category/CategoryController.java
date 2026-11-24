package com.waste2value.category;

import com.waste2value.category.dto.CategoryRequest;
import com.waste2value.category.dto.CategoryResponse;
import com.waste2value.category.dto.CategoryStatusRequest;
import com.waste2value.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Waste Categories", description = "Hierarchical waste category management and public browsing")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "List categories (public: hierarchical tree or flat list)")
    public ResponseEntity<ApiResponse<Object>> getCategories(
            @RequestParam(defaultValue = "true") boolean hierarchical,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        if (hierarchical) {
            return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryTree(activeOnly)));
        } else {
            return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategoriesFlat(activeOnly)));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category details by ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Admin: Create a new waste category or subcategory")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created, "Category created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Admin: Update an existing category")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse updated = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Category updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Admin: Activate or deactivate a category")
    public ResponseEntity<ApiResponse<CategoryResponse>> setCategoryStatus(
            @PathVariable Long id,
            @Valid @RequestBody CategoryStatusRequest request) {
        CategoryResponse updated = categoryService.setCategoryStatus(id, request.active());
        return ResponseEntity.ok(ApiResponse.success(updated, "Category status updated successfully"));
    }
}
