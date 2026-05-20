package com.visco.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visco.backend.models.entities.Category;
import com.visco.backend.repositories.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration webSecurityConfiguration;

    @Test
    void getAllCategories_ReturnsPage() throws Exception {
        Category category = new Category();
        category.setId(1L);
        category.setName("Test Category");

        Page<Category> page = new PageImpl<>(List.of(category));
        when(categoryRepository.findAll(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/inventory/categories").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Test Category"));
    }

    @Test
    void getCategoryById_Success() throws Exception {
        Category category = new Category();
        category.setId(1L);
        category.setName("Test Category");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        mockMvc.perform(get("/api/inventory/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Category"));
    }

    @Test
    void getCategoryById_FailsWhenNotFound() throws Exception {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/inventory/categories/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCategory_Success() throws Exception {
        Category category = new Category();
        category.setName("New Category");

        Category saved = new Category();
        saved.setId(1L);
        saved.setName("New Category");

        when(categoryRepository.findByName("New Category")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        mockMvc.perform(post("/api/inventory/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(category)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Category"));
    }

    @Test
    void createCategory_FailsWhenNameExists() throws Exception {
        Category category = new Category();
        category.setName("Existing Category");

        Category existing = new Category();
        existing.setId(1L);
        existing.setName("Existing Category");

        when(categoryRepository.findByName("Existing Category")).thenReturn(Optional.of(existing));

        mockMvc.perform(post("/api/inventory/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(category)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCategory_Success() throws Exception {
        Category existing = new Category();
        existing.setId(1L);
        existing.setName("Old Name");

        Category updated = new Category();
        updated.setName("New Name");

        Category saved = new Category();
        saved.setId(1L);
        saved.setName("New Name");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByName("New Name")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        mockMvc.perform(put("/api/inventory/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    void deleteCategory_Success() throws Exception {
        Category category = new Category();
        category.setId(1L);
        category.setName("Test Category");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        doNothing().when(categoryRepository).delete(category);

        mockMvc.perform(delete("/api/inventory/categories/1"))
                .andExpect(status().isNoContent());

        verify(categoryRepository).delete(category);
    }
}
