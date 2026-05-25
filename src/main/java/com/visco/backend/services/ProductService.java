package com.visco.backend.services;

import com.visco.backend.models.dtos.CreateProductRequest;
import com.visco.backend.models.dtos.ProductDTO;
import com.visco.backend.models.entities.Category;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.models.entities.Uom;
import com.visco.backend.repositories.CategoryRepository;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.StockLevelRepository;
import com.visco.backend.repositories.SupplierRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

  private final ProductRepository productRepository;
  private final StockLevelRepository stockLevelRepository;
  private final CategoryRepository categoryRepository;
  private final SupplierRepository supplierRepository;

  @Transactional
  public ProductDTO createProduct(CreateProductRequest dto) {
    if (productRepository.findBySku(dto.sku()).isPresent()) {
      throw new IllegalArgumentException("The product is already registered");
    }

    String nextCode = generateNextInternalCode();

    Supplier supplier = null;
    if (dto.supplierId() != null) {
      supplier = supplierRepository
        .findById(dto.supplierId())
        .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
    }

    Category category = null;
    if (dto.categoryId() != null) {
      category = categoryRepository
        .findById(dto.categoryId())
        .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    Product product = Product.builder()
      .internalCode(nextCode)
      .sku(dto.sku())
      .name(dto.name())
      .description(dto.description())
      .sapCode(dto.sapCode() != null ? dto.sapCode() : "")
      .uom(dto.uom())
      .reorderPoint(dto.reorderPoint())
      .supplier(supplier)
      .category(category)
      .build();

    Product savedProduct = productRepository.save(product);

    // Producto nuevo: todos los stocks en 0
    return ProductDTO.fromEntity(
      savedProduct,
      BigDecimal.ZERO,
      BigDecimal.ZERO
    );
  }

  private String generateNextInternalCode() {
    Long nextVal = productRepository.getNextInternalCodeSequence();
    return String.format("%06d", nextVal);
  }

  // ─────────────────────────────────────────────────────────────
  // Stock helpers — fuente de verdad: currentStock, pendingStock
  // ─────────────────────────────────────────────────────────────

  private BigDecimal getTotalStock(Long productId) {
    BigDecimal v = stockLevelRepository.getTotalStockByProductId(productId);
    return v != null ? v : BigDecimal.ZERO;
  }

  private BigDecimal getTotalPendingStock(Long productId) {
    BigDecimal v = stockLevelRepository.getTotalPendingStockByProductId(
      productId
    );
    return v != null ? v : BigDecimal.ZERO;
  }

  // ─────────────────────────────────────────────────────────────
  // Queries públicas
  // ─────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public ProductDTO getProductByInternalCode(String internalCode) {
    Product product = productRepository
      .findByInternalCode(internalCode)
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Producto no encontrado con código interno: " + internalCode
        )
      );
    return ProductDTO.fromEntity(
      product,
      getTotalStock(product.getId()),
      getTotalPendingStock(product.getId())
    );
  }

  @Transactional(readOnly = true)
  public Page<ProductDTO> getProducts(
    Pageable pageable,
    String search,
    String category,
    String sortBy,
    String sortDir
  ) {
    if ("stock".equals(sortBy)) {
      List<Product> allProducts = productRepository.findBySearchAndCategory(
        Pageable.unpaged(), search, category
      ).getContent();

      List<Long> productIds = allProducts.stream().map(Product::getId).toList();

      Map<Long, BigDecimal[]> stockMap = stockLevelRepository
        .sumStockByProductIds(productIds)
        .stream()
        .collect(
          Collectors.toMap(
            row -> (Long) row[0],
            row -> new BigDecimal[] {
              row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO,
              row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO,
            }
          )
        );

      Comparator<ProductDTO> comparator = Comparator.comparing(
        ProductDTO::getTotalStock
      );
      if ("desc".equalsIgnoreCase(sortDir)) {
        comparator = comparator.reversed();
      }

      List<ProductDTO> allDtos = allProducts.stream()
        .map(product -> {
          BigDecimal[] stocks = stockMap.getOrDefault(
            product.getId(),
            new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO }
          );
          return ProductDTO.fromEntity(product, stocks[0], stocks[1]);
        })
        .sorted(comparator)
        .toList();

      long total = allDtos.size();
      int start = (int) pageable.getOffset();
      int end = Math.min(start + pageable.getPageSize(), allDtos.size());
      List<ProductDTO> pageContent = start >= allDtos.size()
        ? List.of()
        : allDtos.subList(start, end);

      return new PageImpl<>(pageContent, pageable, total);
    }

    Page<Product> products = productRepository.findBySearchAndCategory(
      pageable,
      search,
      category
    );

    List<Long> productIds = products.stream().map(Product::getId).toList();

    Map<Long, BigDecimal[]> stockMap = stockLevelRepository
      .sumStockByProductIds(productIds)
      .stream()
      .collect(
        Collectors.toMap(
          row -> (Long) row[0],
          row ->
            new BigDecimal[] {
              row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO,
              row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO,
            }
        )
      );

    List<ProductDTO> dtos = products
      .stream()
      .map(product -> {
        BigDecimal[] stocks = stockMap.getOrDefault(
          product.getId(),
          new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO }
        );
        return ProductDTO.fromEntity(product, stocks[0], stocks[1]);
      })
      .toList();

    return new PageImpl<>(dtos, pageable, products.getTotalElements());
  }

  @Transactional(readOnly = true)
  public ProductDTO getProductById(Long id) {
    Product product = productRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Producto no encontrado: " + id)
      );
    return ProductDTO.fromEntity(
      product,
      getTotalStock(product.getId()),
      getTotalPendingStock(product.getId())
    );
  }

  @Transactional
  public ProductDTO updateProduct(Long id, ProductDTO dto) {
    Product existing = productRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Producto no encontrado: " + id)
      );

    if (
      dto.getSku() != null &&
      !dto.getSku().equals(existing.getSku()) &&
      productRepository.findBySku(dto.getSku()).isPresent()
    ) {
      throw new IllegalArgumentException("El SKU ya existe");
    }

    Supplier supplier = null;
    if (dto.getSupplierId() != null) {
      supplier = supplierRepository
        .findById(dto.getSupplierId())
        .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
    }

    Category category = null;
    if (dto.getCategoryId() != null) {
      category = categoryRepository
        .findById(dto.getCategoryId())
        .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    existing.setName(dto.getName());
    existing.setSku(dto.getSku());
    existing.setDescription(dto.getDescription());
    existing.setSapCode(dto.getSapCode());
    existing.setUom(Uom.valueOf(dto.getUom()));
    existing.setReorderPoint(dto.getReorderPoint());
    existing.setSupplier(supplier);
    existing.setCategory(category);

    Product saved = productRepository.save(existing);
    return ProductDTO.fromEntity(
      saved,
      getTotalStock(saved.getId()),
      getTotalPendingStock(saved.getId())
    );
  }

  @Transactional
  public void deleteProduct(Long id) {
    Product product = productRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Producto no encontrado: " + id)
      );
    product.setActive(false);
    productRepository.save(product);
  }

  @Transactional
  public Category createCategory(Category category) {
    return categoryRepository.save(category);
  }

  @Transactional(readOnly = true)
  public Page<ProductDTO> getProductsByCategory(
    Long categoryId,
    Pageable pageable
  ) {
    return productRepository
      .findByCategoryId(categoryId, pageable)
      .map(product ->
        ProductDTO.fromEntity(
          product,
          getTotalStock(product.getId()),
          getTotalPendingStock(product.getId())
        )
      );
  }

  @Transactional(readOnly = true)
  public Page<ProductDTO> getProductsByWarehouse(
    Long warehouseId,
    Pageable pageable
  ) {
    return productRepository
      .findByWarehouse(warehouseId, pageable)
      .map(product ->
        ProductDTO.fromEntity(
          product,
          getTotalStock(product.getId()),
          getTotalPendingStock(product.getId())
        )
      );
  }
}
