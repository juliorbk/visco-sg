package com.visco.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visco.backend.models.dtos.ProductDTO;
import com.visco.backend.services.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration webSecurityConfiguration;

    @Test
    void getAllProducts_ReturnsPage() throws Exception {
        ProductDTO product = ProductDTO.builder()
                .id(1L).sku("SKU-001").name("Test Product").uom("UNIT").active(true)
                .totalStock(BigDecimal.ZERO).totalPendingStock(BigDecimal.ZERO).build();

        Page<ProductDTO> page = new PageImpl<>(List.of(product));
        when(productService.getAllProducts(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/inventory/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Test Product"));

        verify(productService).getAllProducts(any(PageRequest.class));
    }

    @Test
    void createProduct_Success() throws Exception {
        ProductDTO request = ProductDTO.builder()
                .sku("SKU-001").name("Test Product").uom("UNIT").active(true)
                .reorderPoint(BigDecimal.valueOf(10)).build();

        ProductDTO response = ProductDTO.builder()
                .id(1L).sku("SKU-001").name("Test Product").uom("UNIT").active(true)
                .totalStock(BigDecimal.ZERO).totalPendingStock(BigDecimal.ZERO).build();

        when(productService.createProduct(any(ProductDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/inventory/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Product"));

        verify(productService).createProduct(any(ProductDTO.class));
    }

    @Test
    void getProductById_Success() throws Exception {
        ProductDTO product = ProductDTO.builder()
                .id(1L).sku("SKU-001").name("Test Product").uom("UNIT").active(true)
                .totalStock(BigDecimal.valueOf(100)).totalPendingStock(BigDecimal.ZERO).build();

        when(productService.getProductById(1L)).thenReturn(product);

        mockMvc.perform(get("/api/inventory/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Product"));

        verify(productService).getProductById(1L);
    }

    @Test
    void updateProduct_Success() throws Exception {
        ProductDTO request = ProductDTO.builder()
                .sku("SKU-002").name("Updated Product").uom("UNIT").build();

        ProductDTO response = ProductDTO.builder()
                .id(1L).sku("SKU-002").name("Updated Product").uom("UNIT").active(true)
                .totalStock(BigDecimal.ZERO).totalPendingStock(BigDecimal.ZERO).build();

        when(productService.updateProduct(eq(1L), any(ProductDTO.class))).thenReturn(response);

        mockMvc.perform(put("/api/inventory/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Product"));

        verify(productService).updateProduct(eq(1L), any(ProductDTO.class));
    }

    @Test
    void deleteProduct_Success() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/inventory/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1L);
    }
}
