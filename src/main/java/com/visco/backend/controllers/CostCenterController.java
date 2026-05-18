package com.visco.backend.controllers;

import com.visco.backend.models.dtos.CostCenterDTO;
import com.visco.backend.services.CostCenterService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cost-centers")
@Tag(name = "Cost Centers", description = "Cost center lookup endpoints")
@RequiredArgsConstructor
public class CostCenterController {

    private final CostCenterService costCenterService;

    // GET /api/cost-centers?page=0&size=20
    @GetMapping
    public ResponseEntity<Page<CostCenterDTO>> getAllCostCenters(Pageable pageable) {
        return ResponseEntity.ok(costCenterService.getCostCenters(pageable));
    }

    // GET /api/cost-centers/all  — sin paginar, para dropdowns del frontend
    @GetMapping("/all")
    public ResponseEntity<List<CostCenterDTO>> getAllUnpaged() {
        return ResponseEntity.ok(costCenterService.getAllCostCenters());
    }

    // GET /api/cost-centers/{id}
    @GetMapping("/{id}")
    public ResponseEntity<CostCenterDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(costCenterService.getCostCenterById(id));
    }
}
