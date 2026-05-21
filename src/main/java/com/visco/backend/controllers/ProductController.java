package com.visco.backend.controllers;

import com.visco.backend.models.dtos.CreateProductRequest;
import com.visco.backend.models.dtos.CreateProductRequest;
import com.visco.backend.models.dtos.ProductDTO;
import com.visco.backend.models.entities.Product;
import com.visco.backend.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  // GET /api/inventory/products?page=0&size=10
  // Lista paginada de productos. Retorna ProductDTO (no expone la entidad).
  @GetMapping("/products")
  public ResponseEntity<Page<ProductDTO>> getProducts(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String search,
    @RequestParam(required = false) String category
  ) {
    Pageable pageable = PageRequest.of(page, size);
    Page<ProductDTO> products = productService.getProducts(
      pageable,
      search,
      category
    );
    return ResponseEntity.ok(products);
  }

  // POST /api/inventory/products
  // Crea un producto. Retorna 201 Created con el DTO.
  @PostMapping("/products")
  public ResponseEntity<ProductDTO> createProduct(
    @Valid @RequestBody CreateProductRequest request
  ) {
    return ResponseEntity.status(HttpStatus.CREATED).body(
      productService.createProduct(request)
    );
  }

  // GET /api/inventory/products/{id}
  @GetMapping("/products/{id}")
  public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
    return ResponseEntity.ok(productService.getProductById(id));
  }

  // PUT /api/inventory/products/{id}
  // Actualiza los campos del producto. Retorna el DTO actualizado.
  @PutMapping("/products/{id}")
  public ResponseEntity<ProductDTO> updateProduct(
    @PathVariable Long id,
    @RequestBody ProductDTO request
  ) {
    return ResponseEntity.ok(productService.updateProduct(id, request));
  }

  // DELETE /api/inventory/products/{id}
  // Soft delete: desactiva el producto. Retorna 204 No Content.
  @DeleteMapping("/products/{id}")
  public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
    productService.deleteProduct(id);
    return ResponseEntity.noContent().build();
  }
}
