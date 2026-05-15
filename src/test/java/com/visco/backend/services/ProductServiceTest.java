package com.visco.backend.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.visco.backend.models.dtos.ProductDTO;
import com.visco.backend.models.entities.Category;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.models.entities.Uom;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.StockLevelRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockLevelRepository stockLevelRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private Supplier supplier;
    private Category category;

    @BeforeEach
    void setUp() {
        supplier = new Supplier();
        supplier.setId(1L);
        supplier.setName("Test Supplier");

        category = new Category();
        category.setId(1L);
        category.setName("Test Category");

        product = Product.builder()
                .id(1L)
                .internalCode("000001")
                .sku("SKU-001")
                .name("Test Product")
                .description("A test product")
                .sapCode("SAP-001")
                .uom(Uom.UNIDAD)
                .reorderPoint(BigDecimal.TEN)
                .active(true)
                .supplier(supplier)
                .category(category)
                .build();
    }

    @Test
    void createProduct_shouldSucceed() {
        Product input = Product.builder()
                .sku("SKU-001")
                .name("Test Product")
                .description("A test product")
                .sapCode("SAP-001")
                .uom(Uom.UNIDAD)
                .reorderPoint(BigDecimal.TEN)
                .supplier(supplier)
                .category(category)
                .build();

        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.createProduct(input);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("000001", result.getInternalCode());
        assertEquals("SKU-001", result.getSku());
    }

    @Test
    void createProduct_shouldThrow_whenSkuExists() {
        Product input = Product.builder().sku("SKU-001").build();
        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.of(product));

        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(input));
        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_shouldSetInternalCode_whenNull() {
        Product savedProduct = Product.builder()
                .id(1L)
                .internalCode(null)
                .sku("SKU-002")
                .name("Test")
                .sapCode("SAP")
                .uom(Uom.UNIDAD)
                .reorderPoint(BigDecimal.ONE)
                .active(true)
                .build();

        when(productRepository.findBySku("SKU-002")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        Product result = productService.createProduct(Product.builder()
                .sku("SKU-002")
                .name("Test")
                .sapCode("SAP")
                .uom(Uom.UNIDAD)
                .reorderPoint(BigDecimal.ONE)
                .build());

        assertEquals("000001", result.getInternalCode());
    }

    @Test
    void getProductByInternalCode_shouldReturnDTO() {
        when(productRepository.findByInternalCode("000001")).thenReturn(Optional.of(product));
        when(stockLevelRepository.getTotalStockByProductId(1L)).thenReturn(BigDecimal.valueOf(50));
        when(stockLevelRepository.findByProductId(1L)).thenReturn(List.of());

        ProductDTO dto = productService.getProductByInternalCode("000001");

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("SKU-001", dto.getSku());
        assertEquals("Test Product", dto.getName());
        assertEquals(BigDecimal.valueOf(50), dto.getTotalStock());
    }

    @Test
    void getProductByInternalCode_shouldThrow_whenNotFound() {
        when(productRepository.findByInternalCode("INVALID")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> productService.getProductByInternalCode("INVALID"));
    }

    @Test
    void getAllProducts_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(product));

        when(productRepository.findAll(pageable)).thenReturn(productPage);
        when(stockLevelRepository.getTotalStockByProductId(1L)).thenReturn(BigDecimal.ZERO);

        Page<ProductDTO> result = productService.getAllProducts(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("SKU-001", result.getContent().get(0).getSku());
    }

    @Test
    void getProductById_shouldReturnDTO() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockLevelRepository.getTotalStockByProductId(1L)).thenReturn(BigDecimal.valueOf(30));
        when(stockLevelRepository.findByProductId(1L)).thenReturn(List.of());

        ProductDTO dto = productService.getProductById(1L);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
    }

    @Test
    void getProductById_shouldThrow_whenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> productService.getProductById(99L));
    }

    @Test
    void updateProduct_shouldSucceed() {
        Product updated = Product.builder()
                .sku("SKU-002")
                .name("Updated Product")
                .description("Updated description")
                .sapCode("SAP-002")
                .uom(Uom.CAJA)
                .reorderPoint(BigDecimal.valueOf(5))
                .supplier(supplier)
                .category(category)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.findBySku("SKU-002")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(stockLevelRepository.getTotalStockByProductId(1L)).thenReturn(BigDecimal.ZERO);

        ProductDTO result = productService.updateProduct(1L, updated);

        assertNotNull(result);
        assertEquals("SKU-002", result.getSku());
        assertEquals("Updated Product", result.getName());
    }

    @Test
    void updateProduct_shouldThrow_whenSkuConflict() {
        Product otherProduct = Product.builder().id(2L).sku("SKU-002").build();
        Product updated = Product.builder().sku("SKU-002").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.findBySku("SKU-002")).thenReturn(Optional.of(otherProduct));

        assertThrows(IllegalArgumentException.class, () -> productService.updateProduct(1L, updated));
    }

    @Test
    void updateProduct_shouldThrow_whenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> productService.updateProduct(99L, product));
    }

    @Test
    void deleteProduct_shouldSetInactive() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.deleteProduct(1L);

        assertFalse(product.getActive());
        verify(productRepository).save(product);
    }

    @Test
    void deleteProduct_shouldThrow_whenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> productService.deleteProduct(99L));
    }
}
