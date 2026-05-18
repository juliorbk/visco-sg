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
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

  private final ProductRepository productRepository;
  private final StockLevelRepository stockLevelRepository;
  private final CategoryRepository categoryRepository;
  private final SupplierRepository supplierRepository;

  // Crea un producto nuevo.
  // Valida SKU único (internalCode se genera automáticamente después del save).
  @Transactional
  public ProductDTO createProduct(ProductDTO dto) {
    // 1. Corregida la sintaxis del IF
    if (productRepository.findBySku(dto.getSku()).isPresent()) {
      throw new IllegalArgumentException("The product is already registered");
    }

    // 2. Generación del código incremental sin prefijos
    String nextCode = generateNextInternalCode();

    // 3. Buscar relaciones (Asegúrate de tener inyectados estos repositorios en tu servicio)
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

    // 4. Construcción de la entidad
    Product product = Product.builder()
      .id(null) // Forzamos a PostgreSQL a usar tu secuencia 'product_code_seq'
      .internalCode(nextCode)
      .sku(dto.getSku())
      .name(dto.getName())
      .description(dto.getDescription())
      .sapCode(dto.getSapCode())
      .uom(Uom.valueOf(dto.getUom()))
      .reorderPoint(dto.getReorderPoint())
      .active(dto.getActive() != null ? dto.getActive() : true)
      .supplier(supplier) // <-- Asignamos el proveedor encontrado
      .category(category) // <-- Asignamos la categoría encontrada
      .build();

    Product savedProduct = productRepository.save(product);

    // Como es un producto nuevo, pasamos BigDecimal.ZERO para los stocks
    return ProductDTO.fromEntity(
      savedProduct,
      BigDecimal.ZERO,
      BigDecimal.ZERO
    );
  }

  private String generateNextInternalCode() {
    Long nextVal = productRepository.getNextInternalCodeSequence();

    // %06d significa: entero, rellenado con ceros a la izquierda hasta tener mínimo 6 dígitos.
    // Si nextVal es 230001 -> "230001"
    // Si en un futuro pasa el millón (1000001) -> "1000001" (crece automáticamente)
    return String.format("%06d", nextVal);
  }

  private BigDecimal getTotalStock(Long productId) {
    BigDecimal stock = stockLevelRepository.getTotalStockByProductId(productId);
    return stock != null ? stock : BigDecimal.ZERO;
  }

  private BigDecimal getTotalPendingStock(Long productId) {
    List<com.visco.backend.models.entities.StockLevel> levels =
      stockLevelRepository.findByProductId(productId);
    if (levels.isEmpty()) return BigDecimal.ZERO;
    return levels
      .stream()
      .map(com.visco.backend.models.entities.StockLevel::getPendingStock)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  // Busca un producto por su código interno (VIS-000001).
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

  // Lista paginada de todos los productos.
  // GET /api/inventory/products?page=0&size=10
  public Page<ProductDTO> getAllProducts(Pageable pageable) {
    return productRepository
      .findAll(pageable)
      .map(p ->
        ProductDTO.fromEntity(
          p,
          getTotalStock(p.getId()),
          getTotalPendingStock(p.getId())
        )
      );
  }

  // Busca un producto por su ID numérico.
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

  // Actualiza los campos editables de un producto.
  // Valida SKU único si el valor cambió. No modifica internalCode.
  @Transactional
  public ProductDTO updateProduct(Long id, Product updated) {
    Product existing = productRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Producto no encontrado: " + id)
      );

    if (
      !existing.getSku().equals(updated.getSku()) &&
      productRepository.findBySku(updated.getSku()).isPresent()
    ) {
      throw new IllegalArgumentException("El SKU ya existe");
    }

    existing.setName(updated.getName());
    existing.setSku(updated.getSku());
    existing.setDescription(updated.getDescription());
    existing.setSapCode(updated.getSapCode());
    existing.setUom(updated.getUom());
    existing.setReorderPoint(updated.getReorderPoint());
    existing.setSupplier(updated.getSupplier());
    existing.setCategory(updated.getCategory());

    Product saved = productRepository.save(existing);
    return ProductDTO.fromEntity(
      saved,
      getTotalStock(saved.getId()),
      getTotalPendingStock(saved.getId())
    );
  }

  // Eliminación lógica: desactiva el producto sin borrarlo de la DB.
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

  public Category createCategory(Category category) {
    return categoryRepository.save(category);
  }
}
