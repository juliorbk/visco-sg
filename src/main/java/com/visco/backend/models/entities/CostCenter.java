package com.visco.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "cost_centers")
public class CostCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Columna: CC (Ej: 100, 101)
    @Column(name = "internal_cc", length = 50)
    private String internalCc;

    // Columna: CENTRO DE COSTOS (Ej: 1907152100) - Este es el identificador principal
    @Column(nullable = false, unique = true, length = 100)
    private String code;

    // Columna: DESCRIPCION COMPLETA (Ej: SUPERINTENDENCIA DE LABORATORIO)
    @Column(name = "full_description", nullable = false)
    private String fullDescription;

    // Columna: DESCRIPCION DIVISION (Ej: SPTCIA. DE LABORATORIO)
    @Column(name = "division_description")
    private String divisionDescription;

    // Columna: COD GER. (Ej: 71)
    @Column(name = "management_code", length = 50)
    private String managementCode;

    // Columna: DESCRIPCION GERENCIA (Ej: GERENCIA DE CALIDAD)
    @Column(name = "management_description")
    private String managementDescription;

    // Columna: COD G.G. (Ej: 90)
    @Column(name = "general_management_code", length = 50)
    private String generalManagementCode;

    // Columna: DESCRIPCION GERENCIA GENERAL (Ej: PRESIDENCIA Y UNIDAD DE STAFF)
    @Column(name = "general_management_description")
    private String generalManagementDescription;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
