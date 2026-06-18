package com.visco.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.visco.backend.models.dtos.CreateProductRequest;
import com.visco.backend.models.dtos.ProductDTO;
import com.visco.backend.models.entities.Category;
import com.visco.backend.models.entities.Currency;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.models.entities.Uom;
import com.visco.backend.repositories.CategoryRepository;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.StockLevelRepository;
import com.visco.backend.repositories.SupplierRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Regression tests for the duplicate-sap_code fix.
 *
 * Production was failing with
 *   duplicate key value violates unique constraint "unique_sap_code"
 * because the service validated SKU uniqueness but not sap_code. The
 * DB-level constraint exists in prod but the service did not surface
 * a friendly error, so the user only saw a generic 409 from the
 * GlobalExceptionHandler.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceSapCodeTest {

  @Mock private ProductRepository productRepository;
  @Mock private StockLevelRepository stockLevelRepository;
  @Mock private CategoryRepository categoryRepository;
  @Mock private SupplierRepository supplierRepository;

  @InjectMocks private ProductService productService;

  private CreateProductRequest request(String sapCode) {
    return new CreateProductRequest(
      "Test Product",
      "SKU-1",
      "desc",
      sapCode,
      Uom.EA,
      BigDecimal.ZERO,
      BigDecimal.valueOf(100),
      1L,
      1L
    );
  }

  @Test
  void createProduct_rejectsDuplicateSapCode() {
    when(productRepository.findFirstBySku("SKU-1")).thenReturn(Optional.empty());
    when(productRepository.findFirstBySapCode("4000030121"))
      .thenReturn(Optional.of(Product.builder().id(99L).build()));

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
      () -> productService.createProduct(request("4000030121")));

    assertEquals("Ya existe un producto con el código SAP 4000030121", ex.getMessage());
    verify(productRepository, never()).save(any(Product.class));
  }

  @Test
  void createProduct_allowsUniqueSapCode() {
    when(productRepository.findFirstBySku("SKU-1")).thenReturn(Optional.empty());
    when(productRepository.findFirstBySapCode("UNIQUE-SAP")).thenReturn(Optional.empty());
    when(productRepository.getNextInternalCodeSequence()).thenReturn(123L);
    when(productRepository.save(any(Product.class)))
      .thenAnswer(inv -> {
        Product p = inv.getArgument(0);
        p.setId(42L);
        return p;
      });
    when(supplierRepository.findById(1L)).thenReturn(Optional.of(
      Supplier.builder()
        .id(1L).name("S1").address("a").email("s1@x").description("d")
        .currency(Currency.USD).active(true).build()
    ));
    when(categoryRepository.findById(1L)).thenReturn(Optional.of(
      Category.builder().id(1L).name("C1").build()
    ));

    ProductDTO result = productService.createProduct(request("UNIQUE-SAP"));
    assertEquals(42L, result.getId());
    assertEquals("UNIQUE-SAP", result.getSapCode());
  }

  @Test
  void createProduct_treatsBlankSapCodeAsNoCheck() {
    when(productRepository.findFirstBySku("SKU-1")).thenReturn(Optional.empty());
    when(productRepository.getNextInternalCodeSequence()).thenReturn(5L);
    when(productRepository.save(any(Product.class)))
      .thenAnswer(inv -> {
        Product p = inv.getArgument(0);
        p.setId(7L);
        return p;
      });
    when(supplierRepository.findById(1L)).thenReturn(Optional.of(
      Supplier.builder()
        .id(1L).name("S1").address("a").email("s1@x").description("d")
        .currency(Currency.USD).active(true).build()
    ));
    when(categoryRepository.findById(1L)).thenReturn(Optional.of(
      Category.builder().id(1L).name("C1").build()
    ));

    // sapCode is @NotBlank so the DTO will reject "" at the controller
    // layer, but if a value happens to be just whitespace the service
    // should not blow up on findFirstBySapCode.
    ProductDTO result = productService.createProduct(request("   "));
    assertEquals(7L, result.getId());
    assertEquals("", result.getSapCode());
    verify(productRepository, never()).findFirstBySapCode(any());
  }
}
