package com.visco.backend.repositories;

import com.visco.backend.models.entities.Supplier;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {}
