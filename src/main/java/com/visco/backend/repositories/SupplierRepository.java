package com.visco.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visco.backend.models.entities.Supplier;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {}
