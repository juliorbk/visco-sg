package com.visco.backend.services;

import com.visco.backend.models.dtos.CreateProductRequest;
import com.visco.backend.models.dtos.ProductDTO;
import com.visco.backend.models.entities.*;
import com.visco.backend.repositories.CategoryRepository;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.StockLevelRepository;
import com.visco.backend.repositories.SupplierRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id(1L)
                .internalCode("000001")
                .sku("SKU-001")
                .name("Test Product")
                .description("Test Description")
                .uom(Uom.UN)
                .reorderPoint(BigDecimal.valueOf(10))
                .active(true)
                .build();
    }

    @Test
    void createProduct_Success() {
        CreateProductRequest request = new CreateProductRequest(
                "Test Product", "SKU-001", "Description", "SAP001",
                Uom.UN, BigDecimal.valueOf(10), null, null
        );

        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.empty());
        when(productRepository.getNextInternalCodeSequence()).thenReturn(1L);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductDTO result = productService.createProduct(request);

        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo("SKU-001");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_FailsWhenSkuExists() {
        CreateProductRequest request = new CreateProductRequest(
                "Test Product", "SKU-001", "Description", "SAP001",
                Uom.UN, BigDecimal.valueOf(10), null, null
        );

        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.of(testProduct));

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void createProduct_WithSupplierAndCategory() {
        Supplier supplier = new Supplier();
        supplier.setId(1L);
        Category category = new Category();
        category.setId(1L);

        CreateProductRequest request = new CreateProductRequest(
                "Test Product", "SKU-001", "Description", "SAP001",
                Uom.UN, BigDecimal.valueOf(10), 1L, 1L
        );

        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.empty());
        when(productRepository.getNextInternalCodeSequence()).thenReturn(1L);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductDTO result = productService.createProduct(request);

        assertThat(result).isNotNull();
        verify(supplierRepository).findById(1L);
        verify(categoryRepository).findById(1L);
    }

    @Test
    void createProduct_FailsWhenSupplierNotFound() {
        CreateProductRequest request = new CreateProductRequest(
                "Test Product", "SKU-001", "Description", "SAP001",
                Uom.UN, BigDecimal.valueOf(10), 999L, null
        );

        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.empty());
        when(productRepository.getNextInternalCodeSequence()).thenReturn(1L);
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Supplier not found");
    }

    @Test
    void createProduct_FailsWhenCategoryNotFound() {
        CreateProductRequest request = new CreateProductRequest(
                "Test Product", "SKU-001", "Description", "SAP001",
                Uom.UN, BigDecimal.valueOf(10), null, 999L
        );

        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.empty());
        when(productRepository.getNextInternalCodeSequence()).thenReturn(1L);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void getProductById_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(stockLevelRepository.getTotalStockByProductId(1L)).thenReturn(BigDecimal.valueOf(100));
        when(stockLevelRepository.findByProductId(1L)).thenReturn(Collections.emptyList());

        ProductDTO result = productService.getProductById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Product");
        assertThat(result.getTotalStock()).isEqualTo(BigDecimal.valueOf(100));
    }

    @Test
    void getProductById_FailsWhenNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Producto no encontrado");
    }

    @Test
    void getProducts_ReturnsPage() {
        Page<Product> page = new PageImpl<>(Collections.singletonList(testProduct));
        when(productRepository.findBySearchAndCategory(any(Pageable.class), any(), any())).thenReturn(page);
        when(stockLevelRepository.sumStockByProductIds(any())).thenReturn(Collections.emptyList());

        Page<ProductDTO> result = productService.getProducts(any(Pageable.class), any(), any());

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void updateProduct_Success() {
        ProductDTO updateDto = ProductDTO.builder()
                .sku("SKU-002")
                .name("Updated Product")
                .description("Updated Description")
                .uom("UN")
                .reorderPoint(BigDecimal.valueOf(20))
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.findBySku("SKU-002")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(stockLevelRepository.getTotalStockByProductId(1L)).thenReturn(BigDecimal.ZERO);
        when(stockLevelRepository.findByProductId(1L)).thenReturn(Collections.emptyList());

        ProductDTO result = productService.updateProduct(1L, updateDto);

        assertThat(result).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_FailsWhenNotFound() {
        ProductDTO updateDto = ProductDTO.builder().sku("SKU-002").name("Updated").uom("UN").build();
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(1L, updateDto))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteProduct_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        productService.deleteProduct(1L);

        assertThat(testProduct.getActive()).isFalse();
        verify(productRepository).save(testProduct);
    }

    @Test
    void deleteProduct_FailsWhenNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getProductByInternalCode_Success() {
        when(productRepository.findByInternalCode("000001")).thenReturn(Optional.of(testProduct));
        when(stockLevelRepository.getTotalStockByProductId(1L)).thenReturn(BigDecimal.valueOf(50));
        when(stockLevelRepository.findByProductId(1L)).thenReturn(Collections.emptyList());

        ProductDTO result = productService.getProductByInternalCode("000001");

        assertThat(result).isNotNull();
        assertThat(result.getInternalCode()).isEqualTo("000001");
    }

    @Test
    void getProductByInternalCode_FailsWhenNotFound() {
        when(productRepository.findByInternalCode("000001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductByInternalCode("000001"))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
