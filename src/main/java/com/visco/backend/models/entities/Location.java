package com.visco.backend.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "locations")
public class Location {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // --- ENLACE CON EL ALMACÉN ---
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "warehouse_id", nullable = false)
  private Warehouse warehouse;

  @Column(nullable = false)
  private String aisle; // Pasillo

  @Column(nullable = false)
  private String shelf; // Estante

  private String binLevel; // Gaveta o Nivel

  @Column(unique = true)
  private String locationCode; // Ej: ALM1-A2-E4

  // Getters y Setters...
}
