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

  @Column(nullable = false)
  private String physicalAddress;

  @Column(nullable = false)
  private String description;

  @Column(name = "sap_center_code")
  private String sapCenterCode;

  @Column(name = "is_active", nullable = false)
  private boolean active;

  // Relación con el usuario responsable del almacén
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "responsible_user_id")
  private User responsibleUser;

  @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @Builder.Default
  private Set<Location> storageLocations = new HashSet<>();
}
