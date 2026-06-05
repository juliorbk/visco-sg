package com.visco.backend.controllers;

import com.visco.backend.models.dtos.CreateProductRequest;
import com.visco.backend.models.dtos.ProductDTO;
import com.visco.backend.services.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Products", description = "Product management endpoints")
public class ProductController {

  private final ProductService productService;

  @GetMapping("/products")
  @Operation(
    summary = "List products",
    description = "Returns a paginated list of products with optional search, category, and stock sorting"
  )
  public ResponseEntity<Page<ProductDTO>> getProducts(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String search,
    @RequestParam(required = false) Long category,
    @RequestParam(required = false) String sortBy,
    @RequestParam(defaultValue = "asc") String sortDir,
    @RequestParam(required = false) Boolean hasStock
  ) {
    if (search != null && search.trim().isEmpty()) search = null;
    if (category != null && category == 0) category = null;
    if (sortBy != null && sortBy.trim().isEmpty()) sortBy = null;

    Pageable pageable = PageRequest.of(page, size);
    Page<ProductDTO> products = productService.getProducts(
      pageable,
      search,
      category,
      sortBy,
      sortDir,
      hasStock
    );
    return ResponseEntity.ok(products);
  }

  @PostMapping("/products")
  @Operation(
    summary = "Create product",
    description = "Creates a new product and returns the created product DTO"
  )
  public ResponseEntity<ProductDTO> createProduct(
    @Valid @RequestBody CreateProductRequest request
  ) {
    return ResponseEntity.status(HttpStatus.CREATED).body(
      productService.createProduct(request)
    );
  }

  @GetMapping("/products/{internalCode}")
  @Operation(
    summary = "Get product by internal code",
    description = "Returns a product by its internal code"
  )
  public ResponseEntity<ProductDTO> getProductByInternalCode(
    @PathVariable String internalCode
  ) {
    return ResponseEntity.ok(
      productService.getProductByInternalCode(internalCode)
    );
  }

  @PutMapping("/products/{id}")
  @Operation(
    summary = "Update product",
    description = "Updates product fields and returns the updated DTO"
  )
  public ResponseEntity<ProductDTO> updateProduct(
    @PathVariable Long id,
    @RequestBody ProductDTO request
  ) {
    return ResponseEntity.ok(productService.updateProduct(id, request));
  }

  @DeleteMapping("/products/{id}")
  @Operation(
    summary = "Delete product",
    description = "Soft deletes a product by deactivating it"
  )
  public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
    productService.deleteProduct(id);
    return ResponseEntity.noContent().build();
  }
}
