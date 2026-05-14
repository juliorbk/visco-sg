package com.visco.backend.models.entities;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE suppliers SET is_active = false, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("is_active = true")
@Table(name = "suppliers")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Supplier {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String address;

  @Column(nullable = false, unique = true)
  private String email;

  // Colección para manejar múltiples teléfonos (sin problemas de equals/hashCode)
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "supplier_phones", joinColumns = @JoinColumn(name = "supplier_id"))
  @Builder.Default
  @Column(name = "phone_number", nullable = false)
  private Set<String> phoneNumbers = new HashSet<>();

  @Builder.Default
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "supplier_representatives", joinColumns = @JoinColumn(name = "supplier_id"), inverseJoinColumns = @JoinColumn(name = "representative_id"))
  private Set<LegalRepresentative> representatives = new HashSet<>();

  @Column(nullable = false, columnDefinition = "TEXT")
  private String description;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreatedDate
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  @LastModifiedDate
  private LocalDateTime updatedAt = LocalDateTime.now();

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "sap_code")
  private String sapCode; // Código del proveedor en SAP, si aplica

  @Column(name = "is_active", nullable = false)
  private Boolean active; // Logical removing flag

  @Enumerated(EnumType.STRING)
  @Column(name = "currency", nullable = false)
  private Currency currency;
}
