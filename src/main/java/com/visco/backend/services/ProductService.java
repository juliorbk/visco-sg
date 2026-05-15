package com.visco.backend.services;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.visco.backend.models.dtos.ProductDTO;
import com.visco.backend.models.entities.Product;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.StockLevelRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StockLevelRepository stockLevelRepository;

    // Crea un producto nuevo.
    // Valida SKU único (internalCode se genera automáticamente después del save).
    @Transactional
    public Product createProduct(Product product) {

        if (productRepository.findBySku(product.getSku()).isPresent()) {
            throw new IllegalArgumentException("El SKU ya existe");
        }

        Product newProduct = Product.builder()
                .name(product.getName())
                .category(product.getCategory())
                .description(product.getDescription())
                .sapCode(product.getSapCode())
                .uom(product.getUom())
                .active(true)
                .reorderPoint(product.getReorderPoint())
                .sku(product.getSku())
                .supplier(product.getSupplier())
                .build();

        Product saved = productRepository.save(newProduct);
        if (saved.getInternalCode() == null || saved.getInternalCode().isBlank()) {
            saved.setInternalCode(String.format("%06d", saved.getId())); // -> Code: 000001+
        }
        return saved;
    }

    private BigDecimal getTotalStock(Long productId) {
        BigDecimal stock = stockLevelRepository.getTotalStockByProductId(productId);
        return stock != null ? stock : BigDecimal.ZERO;
    }

    private BigDecimal getTotalPendingStock(Long productId) {
        List<com.visco.backend.models.entities.StockLevel> levels = stockLevelRepository.findByProductId(productId);
        if (levels.isEmpty())
            return BigDecimal.ZERO;
        return levels.stream()
                .map(com.visco.backend.models.entities.StockLevel::getPendingStock)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Busca un producto por su código interno (VIS-000001).
    public ProductDTO getProductByInternalCode(String internalCode) {
        Product product = productRepository.findByInternalCode(internalCode)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Producto no encontrado con código interno: " + internalCode));
        return ProductDTO.fromEntity(product, getTotalStock(product.getId()), getTotalPendingStock(product.getId()));
    }

    // Lista paginada de todos los productos.
    // GET /api/inventory/products?page=0&size=10
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(p -> ProductDTO.fromEntity(p, getTotalStock(p.getId()), getTotalPendingStock(p.getId())));
    }

    // Busca un producto por su ID numérico.
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + id));
        return ProductDTO.fromEntity(product, getTotalStock(product.getId()), getTotalPendingStock(product.getId()));
    }

    // Actualiza los campos editables de un producto.
    // Valida SKU único si el valor cambió. No modifica internalCode.
    @Transactional
    public ProductDTO updateProduct(Long id, Product updated) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + id));

        if (!existing.getSku().equals(updated.getSku())
                && productRepository.findBySku(updated.getSku()).isPresent()) {
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
        return ProductDTO.fromEntity(saved, getTotalStock(saved.getId()), getTotalPendingStock(saved.getId()));
    }

    // Eliminación lógica: desactiva el producto sin borrarlo de la DB.
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + id));
        product.setActive(false);
        productRepository.save(product);
    }
}
