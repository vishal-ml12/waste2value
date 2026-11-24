package com.waste2value.category;

import com.waste2value.category.dto.CategoryRequest;
import com.waste2value.category.dto.CategoryResponse;
import com.waste2value.category.dto.CategoryTreeResponse;
import com.waste2value.shared.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository);
    }

    @Test
    void createCategory_RootCategory_Success() {
        CategoryRequest request = new CategoryRequest(null, "Plastic", null, "Recyclable polymers", "kg", true);
        when(categoryRepository.existsByNameIgnoreCaseAndParentCategoryIsNull("Plastic")).thenReturn(false);
        when(categoryRepository.existsBySlug("plastic")).thenReturn(false);

        Category saved = new Category(null, "Plastic", "plastic", "Recyclable polymers", "kg", true);
        saved.setId(1L);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponse response = categoryService.createCategory(request);

        assertNotNull(response);
        assertEquals("Plastic", response.name());
        assertEquals("plastic", response.slug());
        assertNull(response.parentId());
    }

    @Test
    void createCategory_DuplicateNameUnderSameParent_ThrowsConflictException() {
        Category parent = new Category(null, "Plastic", "plastic", "Polymer", "kg", true);
        parent.setId(1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(categoryRepository.existsByNameIgnoreCaseAndParentCategoryId("PET Bottles", 1L)).thenReturn(true);

        CategoryRequest request = new CategoryRequest(1L, "PET Bottles", null, "Clear bottles", "kg", true);

        ConflictException ex = assertThrows(ConflictException.class, () -> categoryService.createCategory(request));
        assertTrue(ex.getMessage().contains("already exists under parent"));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createCategory_SubCategory_Success() {
        Category parent = new Category(null, "Plastic", "plastic", "Polymer", "kg", true);
        parent.setId(1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(categoryRepository.existsByNameIgnoreCaseAndParentCategoryId("HDPE", 1L)).thenReturn(false);
        when(categoryRepository.existsBySlug("hdpe")).thenReturn(false);

        Category sub = new Category(parent, "HDPE", "hdpe", "High density", "kg", true);
        sub.setId(2L);
        when(categoryRepository.save(any(Category.class))).thenReturn(sub);

        CategoryRequest request = new CategoryRequest(1L, "HDPE", null, "High density", "kg", true);
        CategoryResponse response = categoryService.createCategory(request);

        assertNotNull(response);
        assertEquals("HDPE", response.name());
        assertEquals(1L, response.parentId());
        assertEquals("Plastic", response.parentName());
    }

    @Test
    void getCategoryTree_ReturnsHierarchicalStructure() {
        Category root = new Category(null, "Plastic", "plastic", "Polymer", "kg", true);
        root.setId(1L);
        Category sub = new Category(root, "PET", "pet", "Bottles", "kg", true);
        sub.setId(2L);
        root.getSubCategories().add(sub);

        when(categoryRepository.findByParentCategoryIsNullAndActiveTrueOrderByNameAsc()).thenReturn(List.of(root));

        List<CategoryTreeResponse> tree = categoryService.getCategoryTree(true);

        assertEquals(1, tree.size());
        assertEquals("Plastic", tree.get(0).name());
        assertEquals(1, tree.get(0).subCategories().size());
        assertEquals("PET", tree.get(0).subCategories().get(0).name());
    }

    @Test
    void setCategoryStatus_TogglesActive() {
        Category cat = new Category(null, "Metal", "metal", "Metals", "kg", true);
        cat.setId(5L);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(cat));
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        CategoryResponse response = categoryService.setCategoryStatus(5L, false);

        assertFalse(response.active());
    }
}
