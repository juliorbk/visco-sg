package com.visco.backend.models.entities;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Supplier {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(nullable = false)
  private String address;

  @Column(nullable = false, unique = true)
  private String email;

  // Colección para manejar múltiples teléfonos (sin problemas de equals/hashCode)
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
    name = "supplier_phones",
    joinColumns = @JoinColumn(name = "supplier_id")
  )
  @Column(name = "phone_number", nullable = false)
  private Set<String> phoneNumbers = new HashSet<>();


  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "supplier_representatives",
    joinColumns = @JoinColumn(name = "supplier_id"),
    inverseJoinColumns = @JoinColumn(name = "representative_id")
  )
  private Set<LegalRepresentative> representatives = new HashSet<>();

  @Column(nullable = false)
  private String description;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "sap_code")
  private String sapCode; // Código del proveedor en SAP, si aplica

  @Column(name = "is_active", nullable = false)
  private boolean active; // Logical removing flag

  @Enumerated(EnumType.STRING)
  @Column(name = "currency", nullable = false)
  private Currency currency;
}
