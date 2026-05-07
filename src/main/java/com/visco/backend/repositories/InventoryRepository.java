package com.visco.backend.repositories;

import com.visco.backend.models.entities.Product;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository
  extends JpaRepository<Product, UUID> {}
