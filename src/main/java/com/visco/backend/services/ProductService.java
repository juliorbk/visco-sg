package com.visco.backend.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.visco.backend.models.dtos.ProductDTO;
import com.visco.backend.models.entities.Product;
import com.visco.backend.repositories.ProductRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

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

        return productRepository.save(newProduct);
    }

    public ProductDTO getProductByInternalCode(String internalCode) {
        Product product = productRepository.findByInternalCode(internalCode)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Producto no encontrado con código interno: " + internalCode));
        return ProductDTO.fromEntity(product);
    }

    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(ProductDTO::fromEntity);
    }

    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + id));
        return ProductDTO.fromEntity(product);
    }

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

        return ProductDTO.fromEntity(productRepository.save(existing));
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + id));
        product.setActive(false);
        productRepository.save(product);
    }
}

        Product newProduct = Product.builder().name(product.getName())
                .category(product.getCategory()).description(product.getDescription())
                .sapCode(product.getSapCode()).uom(product.getUom()).active(true)
                .internalCode(product.getInternalCode()).reorderPoint(product.getReorderPoint())
                .sku(product.getSku()).supplier(product.getSupplier()).build();

        return productRepository.save(newProduct);

    }

    public Product updateProduct(Long id, Product productDetails) {

        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id));

        // 1. Standard text/number fields
        existingProduct.setName(productDetails.getName());
        existingProduct.setDescription(productDetails.getDescription());
        existingProduct.setSapCode(productDetails.getSapCode());
        existingProduct.setUom(productDetails.getUom());
        existingProduct.setReorderPoint(productDetails.getReorderPoint());
        existingProduct.setSku(productDetails.getSku());

        // 2. Relationships
        existingProduct.setCategory(productDetails.getCategory());
        existingProduct.setSupplier(productDetails.getSupplier());

        // 3. Boolean safety check
        if (productDetails.getActive() != null) {
            existingProduct.setActive(productDetails.getActive());
        }

        return productRepository.save(existingProduct);
    }

    public void deleteProduct(Long id) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id));
        existingProduct.setActive(false); // Logical delete
        productRepository.save(existingProduct);
    }

    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        // 1. Pass the pageable request to the repository
        // 2. The repository returns a Page<Product>
        // 3. Page has a built-in .map() to convert the entities to DTOs
        return productRepository.findAll(pageable).map(ProductDTO::fromEntity);
    }
}