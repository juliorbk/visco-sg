package com.visco.backend.models.entities;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Warehouse {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(unique = true, nullable = false)
  private String name;

  // Esta es la dirección física del edificio en Ciudad Guayana (ej. Zona Industrial Matanzas)
  @Column(nullable = false)
  private String physicalAddress;

  @Column(nullable = false)
  private String description;

  @Column(name = "sap_center_code")
  private String sapCenterCode; // Código del almacén en SAP, si aplica

  @Column(name = "is_active", nullable = false)
  private boolean active;

  // Relación Bidireccional: Un almacén tiene muchas ubicaciones internas (pasillos/estantes)
  @OneToMany(
    mappedBy = "warehouse",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  @EqualsAndHashCode.Exclude // Evita ciclos infinitos en Lombok
  @ToString.Exclude // Evita ciclos infinitos al imprimir el objeto
  @Builder.Default
  private Set<Location> storageLocations = new HashSet<>();
}
