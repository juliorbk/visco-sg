package com.visco.backend.repositories;

import com.visco.backend.models.entities.RequestingArea;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaRepository extends JpaRepository<RequestingArea, Long> {}
