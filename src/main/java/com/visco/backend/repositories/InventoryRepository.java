package com.visco.backend.repositories;

import com.visco.backend.models.entities.InventoryItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository
  extends JpaRepository<InventoryItem, UUID> {}
