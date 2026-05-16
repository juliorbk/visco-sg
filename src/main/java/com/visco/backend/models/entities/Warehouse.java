package com.visco.backend.models.entities;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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

  // Esta es la dirección física del edificio en Ciudad Guayana (ej. Zona
  // Industrial Matanzas)
  @Column(nullable = false)
  private String physicalAddress;

  @Column(nullable = false)
  private String description;

  @Column(name = "sap_center_code")
  private String sapCenterCode; // Código del almacén en SAP, si aplica

  @Column(name = "is_active", nullable = false)
  private boolean active;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "responsible_user_id", nullable = false)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private User responsibleUser;
  // Relación Bidireccional: Un almacén tiene muchas ubicaciones internas
  // (pasillos/estantes)
  @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
  @EqualsAndHashCode.Exclude // Evita ciclos infinitos en Lombok
  @ToString.Exclude // Evita ciclos infinitos al imprimir el objeto
  @Builder.Default
  private Set<Location> storageLocations = new HashSet<>();
}
