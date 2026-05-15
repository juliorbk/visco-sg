package com.visco.backend.controllers;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.visco.backend.models.dtos.ProductDTO;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.Uom;
import com.visco.backend.repositories.UserRepository;
import com.visco.backend.services.JwtService;
import com.visco.backend.services.ProductService;

import jakarta.persistence.EntityNotFoundException;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private ProductDTO createDTO(Long id) {
        return ProductDTO.builder()
                .id(id).internalCode(String.format("%06d", id))
                .sku("SKU-" + id).name("Product " + id)
                .uom("UNIDAD").reorderPoint(BigDecimal.TEN)
                .totalStock(BigDecimal.valueOf(50))
                .totalPendingStock(BigDecimal.ZERO).active(true)
                .build();
    }

    @Test
    @WithMockUser(authorities = "WAREHOUSEMAN")
    void getAllProducts_shouldReturn200() throws Exception {
        when(productService.getAllProducts(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(createDTO(1L))));

        mockMvc.perform(get("/api/inventory/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("SKU-1"));
    }

    @Test
    @WithMockUser(authorities = "WAREHOUSEMAN")
    void createProduct_shouldReturn201() throws Exception {
        when(productService.createProduct(any(Product.class))).thenReturn(Product.builder().id(1L).build());
        when(productService.getProductById(anyLong())).thenReturn(createDTO(1L));

        mockMvc.perform(post("/api/inventory/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-NEW\",\"name\":\"New\",\"sapCode\":\"SAP\",\"uom\":\"UNIDAD\",\"reorderPoint\":10}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "WAREHOUSEMAN")
    void createProduct_shouldReturn400_whenDuplicateSku() throws Exception {
        when(productService.createProduct(any(Product.class)))
                .thenThrow(new IllegalArgumentException("El SKU ya existe"));

        mockMvc.perform(post("/api/inventory/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-EX\",\"name\":\"P\",\"sapCode\":\"S\",\"uom\":\"UNIDAD\",\"reorderPoint\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "WAREHOUSEMAN")
    void getProductById_shouldReturn200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(createDTO(1L));

        mockMvc.perform(get("/api/inventory/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-1"));
    }

    @Test
    @WithMockUser(authorities = "WAREHOUSEMAN")
    void getProductById_shouldReturn404() throws Exception {
        when(productService.getProductById(99L)).thenThrow(new EntityNotFoundException("Not found"));

        mockMvc.perform(get("/api/inventory/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "WAREHOUSEMAN")
    void updateProduct_shouldReturn200() throws Exception {
        ProductDTO dto = createDTO(1L);
        dto.setName("Updated");
        when(productService.updateProduct(eq(1L), any(Product.class))).thenReturn(dto);

        mockMvc.perform(put("/api/inventory/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1\",\"name\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    @WithMockUser(authorities = "WAREHOUSEMAN")
    void updateProduct_shouldReturn400_whenSkuConflict() throws Exception {
        when(productService.updateProduct(eq(1L), any(Product.class)))
                .thenThrow(new IllegalArgumentException("El SKU ya existe"));

        mockMvc.perform(put("/api/inventory/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-CONFLICT\",\"name\":\"Test\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "WAREHOUSEMAN")
    void deleteProduct_shouldReturn204() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/inventory/products/1"))
                .andExpect(status().isNoContent());
    }

}
