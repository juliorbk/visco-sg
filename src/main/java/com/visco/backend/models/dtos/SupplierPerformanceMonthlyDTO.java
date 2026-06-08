package com.visco.backend.models.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
// DTO comparing supplier fulfillment rates across performance tiers.
public class SupplierPerformanceMonthlyDTO {
    private String month;
    private double a; // Tier 1: promedio fulfillment top proveedores
    private double b; // Tier 2-3: promedio fulfillment resto
}