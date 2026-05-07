package com.visco.backend.repositories;

import com.visco.backend.models.entities.RequestingArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestingAreaRepository
  extends JpaRepository<RequestingArea, Long> {}
