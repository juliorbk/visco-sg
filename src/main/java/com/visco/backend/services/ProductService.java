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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles business logic for product and stock operations.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

  private final ProductRepository productRepository;
  private final StockLevelRepository stockLevelRepository;
  private final CategoryRepository categoryRepository;
  private final SupplierRepository supplierRepository;

  // ─────────────────────────────────────────────────────────────
  // Shared page mapping helper — single source of truth
  // Eliminates duplicated stock-map logic and does one batch
  // query per page instead of N individual queries.
  // ─────────────────────────────────────────────────────────────

  private Page<ProductDTO> toProductDTOPage(
    Page<Product> products,
    Pageable pageable
  ) {
    List<Long> ids = products.stream().map(Product::getId).toList();

    if (ids.isEmpty()) {
      return Page.empty(pageable);
    }

    Map<Long, BigDecimal[]> stockMap = stockLevelRepository
      .sumStockByProductIds(ids)
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

    // Explicit hint to GC — stockMap can be large and is no longer needed
    stockMap.clear();

    return new PageImpl<>(dtos, pageable, products.getTotalElements());
  }

  // ─────────────────────────────────────────────────────────────
  // Stock helpers — used only for single-product lookups
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
  // Writes
  // ─────────────────────────────────────────────────────────────

  /**
   * Creates a new product with a generated internal code.
   *
   * @param dto the product creation request
   * @return the created product DTO
   */
  @Transactional
  public ProductDTO createProduct(CreateProductRequest dto) {
    if (productRepository.findFirstBySku(dto.sku()).isPresent()) {
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
      .maxStock(dto.maxStock())
      .supplier(supplier)
      .category(category)
      .build();

    Product savedProduct = productRepository.save(product);

    // New product — all stocks are zero, no DB query needed
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

  /**
   * Updates an existing product's details and stock references.
   *
   * @param id  the product ID
   * @param dto the product update data
   * @return the updated product DTO
   */
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
      productRepository.findFirstBySku(dto.getSku()).isPresent()
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
    if (dto.getMaxStock() != null) existing.setMaxStock(dto.getMaxStock());
    existing.setSupplier(supplier);
    existing.setCategory(category);

    Product saved = productRepository.save(existing);
    return ProductDTO.fromEntity(
      saved,
      getTotalStock(saved.getId()),
      getTotalPendingStock(saved.getId())
    );
  }

  /**
   * Soft-deletes a product by setting it inactive.
   *
   * @param id the product ID
   */
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

  /**
   * Creates a new category entity directly.
   *
   * @param category the category entity to save
   * @return the saved category
   */
  @Transactional
  public Category createCategory(Category category) {
    return categoryRepository.save(category);
  }

  // ─────────────────────────────────────────────────────────────
  // Reads
  // ─────────────────────────────────────────────────────────────

  /**
   * Retrieves a product by its internal code with stock information.
   *
   * @param internalCode the product internal code
   * @return the product DTO with stock data
   */
  @Transactional(readOnly = true)
  public ProductDTO getProductByInternalCode(String internalCode) {
    Product product = productRepository
      .findFirstByInternalCode(internalCode)
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

  /**
   * Retrieves a product by its ID with stock information.
   *
   * @param id the product ID
   * @return the product DTO with stock data
   */
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

  /**
   * Retrieves a paginated, filterable, and sortable list of products.
   *
   * @param pageable pagination information
   * @param search   optional search term
   * @param category optional category filter
   * @param sortBy   optional sort field
   * @param sortDir  sort direction (asc/desc)
   * @param hasStock filter products with stock only
   * @return page of product DTOs
   */
  @Transactional(readOnly = true)
  public Page<ProductDTO> getProducts(
    Pageable pageable,
    String search,
    Long category,
    String sortBy,
    String sortDir,
    Boolean hasStock
  ) {
    Page<Product> products;

    if (Boolean.TRUE.equals(hasStock)) {
      if ("stock".equals(sortBy)) {
        products = "desc".equalsIgnoreCase(sortDir)
          ? productRepository.findBySearchAndCategoryWithStockOrderByStockDesc(
              pageable,
              search,
              category
            )
          : productRepository.findBySearchAndCategoryWithStockOrderByStockAsc(
              pageable,
              search,
              category
            );
      } else {
        products = productRepository.findBySearchAndCategoryWithStock(
          pageable,
          search,
          category
        );
      }
    } else {
      if ("stock".equals(sortBy)) {
        products = "desc".equalsIgnoreCase(sortDir)
          ? productRepository.findBySearchAndCategoryOrderByStockDesc(
              pageable,
              search,
              category
            )
          : productRepository.findBySearchAndCategoryOrderByStockAsc(
              pageable,
              search,
              category
            );
      } else {
        products = productRepository.findBySearchAndCategory(
          pageable,
          search,
          category
        );
      }
    }

    return toProductDTOPage(products, pageable);
  }

  /**
   * Retrieves products filtered by category.
   *
   * @param categoryId the category ID
   * @param pageable   pagination information
   * @return page of product DTOs
   */
  @Transactional(readOnly = true)
  public Page<ProductDTO> getProductsByCategory(
    Long categoryId,
    Pageable pageable
  ) {
    // Fixed: was doing one getTotalStock() + getTotalPendingStock() query per
    // product (N+1). Now uses a single batch query via toProductDTOPage().
    Page<Product> products = productRepository.findByCategoryIdWithFetch(
      categoryId,
      pageable
    );
    return toProductDTOPage(products, pageable);
  }

  /**
   * Retrieves products available in a specific warehouse.
   *
   * @param warehouseId the warehouse ID
   * @param pageable    pagination information
   * @return page of product DTOs
   */
  @Transactional(readOnly = true)
  public Page<ProductDTO> getProductsByWarehouse(
    Long warehouseId,
    Pageable pageable
  ) {
    // Fixed: same N+1 issue as getProductsByCategory above.
    Page<Product> products = productRepository.findByWarehouse(
      warehouseId,
      pageable
    );
    return toProductDTOPage(products, pageable);
  }
}
