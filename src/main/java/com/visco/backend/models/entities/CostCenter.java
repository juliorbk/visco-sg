package com.visco.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

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

  // Columna: CENTRO DE COSTOS (Ej: 1907152100) - Este es el identificador principal
  @Column(nullable = false, unique = true, length = 100)
  private String code;

  // Columna: DESCRIPCION COMPLETA (Ej: SUPERINTENDENCIA DE LABORATORIO)
  @Column(name = "full_description", nullable = false)
  private String fullDescription;

  // Columna: DESCRIPCION DIVISION (Ej: SPTCIA. DE LABORATORIO)
  @Column(name = "division_description")
  private String divisionDescription;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private boolean active = true;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "management_id", nullable = false)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private Management management;
}
