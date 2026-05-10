package com.visco.backend.services;

import org.springframework.stereotype.Service;
import com.visco.backend.models.entities.Product;
import com.visco.backend.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Product createProduct(Product product) {

        if (productRepository.findByInternalCode(product.getInternalCode()).isPresent()) {
            throw new IllegalArgumentException("El código interno ya existe");
        }

        Product newProduct = Product.builder().name(product.getName())
                .category(product.getCategory()).description(product.getDescription())
                .sapCode(product.getSapCode()).uom(product.getUom()).active(true)
                .internalCode(product.getInternalCode()).reorderPoint(product.getReorderPoint())
                .sku(product.getSku()).supplier(product.getSupplier()).build();

        return productRepository.save(newProduct);


    }
}
