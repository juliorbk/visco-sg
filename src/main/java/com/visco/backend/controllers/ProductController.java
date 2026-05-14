package com.visco.backend.controllers;

import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.RestController;

import com.visco.backend.models.dtos.ProductDTO;
import com.visco.backend.models.entities.Product;
import com.visco.backend.services.ProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class ProductController {
	private final ProductService productService;

	// GET /api/inventory/products?page=0&size=10
	// Lista paginada de productos. Retorna ProductDTO (no expone la entidad).
	@GetMapping("/products")
	public Page<ProductDTO> getAllProducts(Pageable pageable) {
		return productService.getAllProducts(pageable);
	}

	// POST /api/inventory/products
	// Crea un producto. Retorna 201 Created con el DTO.
	// Si hay duplicado de SKU → 400 Bad Request.
	@PostMapping("/products")
	public ResponseEntity<ProductDTO> createProduct(@RequestBody Product product) {
		try {
			Product createdProduct = productService.createProduct(product);
			ProductDTO dto = productService.getProductById(createdProduct.getId());
			return ResponseEntity.status(HttpStatus.CREATED).body(dto);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		}
	}

	// GET /api/inventory/products/{id}
	@GetMapping("/products/{id}")
	public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
		return ResponseEntity.ok(productService.getProductById(id));
	}

	// PUT /api/inventory/products/{id}
	// Actualiza los campos del producto. Retorna el DTO actualizado.
	@PutMapping("/products/{id}")
	public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @RequestBody Product product) {
		try {
			return ResponseEntity.ok(productService.updateProduct(id, product));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		}
	}

	// DELETE /api/inventory/products/{id}
	// Soft delete: desactiva el producto. Retorna 204 No Content.
	@DeleteMapping("/products/{id}")
	public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
		productService.deleteProduct(id);
		return ResponseEntity.noContent().build();
	}
}
