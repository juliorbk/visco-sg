package com.visco.backend.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.visco.backend.models.dtos.SupplierDTO;
import com.visco.backend.models.entities.Currency;
import com.visco.backend.models.entities.Supplier;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
	Page<Supplier> findAll(Pageable pageable);

	Page<SupplierDTO> findByActiveTrue(Pageable pageable);

	Page<SupplierDTO> findByActiveFalse(Pageable pageable);

	Page<SupplierDTO> findByCurrency(Currency currency, Pageable pageable); // <--- NEW <--->
}
