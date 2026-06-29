package com.visco.backend.services;

import com.visco.backend.models.dtos.CreateSupplierRequest;
import com.visco.backend.models.dtos.SupplierDTO;
import com.visco.backend.models.dtos.SupplierPerformanceDTO;
import com.visco.backend.models.dtos.SupplierPerformanceMonthlyDTO;
import com.visco.backend.models.dtos.UpdateSupplierRequest;
import com.visco.backend.models.entities.Currency;
import com.visco.backend.models.entities.LegalRepresentative;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.models.entities.SupplierCategory;
import com.visco.backend.repositories.PurchaseOrderRepository;
import com.visco.backend.repositories.SupplierCategoryRepository;
import com.visco.backend.repositories.SupplierRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles business logic for supplier management operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SupplierService {

  private final SupplierRepository supplierRepository;
  private final PurchaseOrderRepository orderRepository;
  private final SupplierCategoryRepository categoryRepository;

  private SupplierCategory resolveCategory(Long categoryId) {
    if (categoryId == null) return null;
    return categoryRepository
      .findById(categoryId)
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Supplier category not found: " + categoryId
        )
      );
  }

  // ------ CRUD ------

  // Create Supplier

  /**
   * Creates a new supplier with optional category and representatives.
   *
   * @param request the supplier creation request
   * @return the created supplier DTO
   */
  @Transactional
  public SupplierDTO createSupplier(CreateSupplierRequest request) {
    if (supplierRepository.existsByName(request.name())) {
      throw new IllegalStateException(
        "Supplier with name: " + request.name() + " already exists"
      );
    }

    // 1. Construyes el proveedor base
    Supplier newSupplier = Supplier.builder()
      .name(request.name())
      .address(request.address())
      .email(request.email())
      .taxId(request.taxId())
      .phoneNumbers(request.phoneNumbers())
      .description(request.description())
      .currency(request.currency())
      .active(true)
      .representatives(new HashSet<>())
      .category(resolveCategory(request.categoryId()))
      .build();

    // 2. Representantes — se crean como entidades independientes
    if (
      request.representativeIds() != null &&
      !request.representativeIds().isEmpty()
    ) {
      Set<LegalRepresentative> reps = request
        .representativeIds()
        .stream()
        .map((String nombre) ->
          LegalRepresentative.builder() // ← tipo explícito
            .fullName(nombre)
            .build()
        )
        .collect(Collectors.toSet());

      newSupplier.setRepresentatives(reps);
    }

    // 3. Guarda y retorna — siempre, con o sin representantes
    return SupplierDTO.fromSupplier(supplierRepository.save(newSupplier));
  }

  // Read all Suppliers
  /**
   * Retrieves a paginated list of suppliers with optional search.
   *
   * @param search   optional search term
   * @param pageable pagination information
   * @return page of supplier DTOs
   */
  @Transactional(readOnly = true)
  public Page<SupplierDTO> getAllSuppliers(String search, Pageable pageable) {
    if (search != null && search.trim().isEmpty()) search = null;
    return supplierRepository
      .findAllWithSearch(search, pageable)
      .map(SupplierDTO::fromSupplier);
  }

  // Update Supplier
  /**
   * Updates an existing supplier's details and category.
   *
   * @param id      the supplier ID
   * @param request the update request
   * @return the updated supplier DTO
   */
  @Transactional
  public SupplierDTO updateSupplier(Long id, UpdateSupplierRequest request) {
    Supplier existing = supplierRepository
      .findByIdWithRepresentatives(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Supplier not found: " + id)
      );

    existing.setName(request.name());
    existing.setEmail(request.email());
    existing.setTaxId(request.taxId());
    existing.setPhoneNumbers(request.phoneNumbers());
    existing.setDescription(request.description());
    existing.setAddress(request.address());
    existing.setCurrency(request.currency());
    existing.setCategory(resolveCategory(request.categoryId()));
    existing.setUpdatedAt(LocalDateTime.now());

    if (request.representativeIds() != null) {
      Set<LegalRepresentative> filtered = existing
        .getRepresentatives()
        .stream()
        .filter(r -> request.representativeIds().contains(r.getId()))
        .collect(Collectors.toSet());
      existing.setRepresentatives(filtered);
    }

    return SupplierDTO.fromSupplier(supplierRepository.save(existing));
  }

  // Deacativate - Delete Suppplier

  /**
   * Soft-deletes a supplier by setting it inactive.
   *
   * @param Id the supplier ID
   */
  @Transactional
  public void deleteSupplier(Long Id) {
    Supplier supplier = supplierRepository
      .findById(Id)
      .orElseThrow(() ->
        new EntityNotFoundException("Supplier not found: " + Id)
      );

    supplier.setActive(false);
    supplier.setDeletedAt(LocalDateTime.now());
    supplierRepository.save(supplier);
    log.info("Soft-deleted (deactivated) supplier with id: {}", Id);
  }

  /**
   * Deactivates an active supplier.
   *
   * @param id the supplier ID
   */
  @Transactional
  public void deactivateSupplier(Long id) {
    // 1. Properly handle the Optional using orElseThrow
    Supplier supplier = supplierRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Supplier not found with id: " + id)
      );

    // 2. Safe boolean check (handles null if active is a Boolean object)
    if (Boolean.FALSE.equals(supplier.getActive())) {
      throw new IllegalStateException(
        "Supplier with id: " + id + " is already inactive"
      );
    }

    log.info("Deactivating supplier with id: {}", id);

    // 3. Update state and timestamps
    supplier.setActive(false);
    supplier.setUpdatedAt(LocalDateTime.now());

    supplierRepository.save(supplier);
  }

  /**
   * Activates an inactive supplier.
   *
   * @param id the supplier ID
   */
  @Transactional
  public void activateSupplier(Long id) {
    Supplier supplier = supplierRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Supplier not found with id: " + id)
      );

    if (Boolean.TRUE.equals(supplier.getActive())) {
      throw new IllegalStateException(
        "Supplier with id: " + id + " is already active"
      );
    }

    log.info("Activating supplier with id: {}", id);

    supplier.setActive(true);
    supplier.setUpdatedAt(LocalDateTime.now());

    supplierRepository.save(supplier);
  }

  /**
   * Retrieves a supplier by its ID.
   *
   * @param id the supplier ID
   * @return the supplier DTO
   */
  @Transactional(readOnly = true)
  public SupplierDTO getSupplierById(Long id) {
    return SupplierDTO.fromSupplier(
      supplierRepository
        .findById(id)
        .orElseThrow(() ->
          new EntityNotFoundException("Supplier not found: " + id)
        )
    );
  }

  /**
   * Retrieves a paginated list of active suppliers.
   *
   * @param pageable pagination information
   * @return page of supplier DTOs
   */
  @Transactional(readOnly = true)
  public Page<SupplierDTO> getActiveSuppliers(Pageable pageable) {
    return supplierRepository
      .findByActiveTrueWithFetch(pageable)
      .map(SupplierDTO::fromSupplier);
  }

  /**
   * Retrieves a paginated list of inactive suppliers.
   *
   * @param pageable pagination information
   * @return page of supplier DTOs
   */
  @Transactional(readOnly = true)
  public Page<SupplierDTO> getInactiveSuppliers(Pageable pageable) {
    return supplierRepository
      .findByActiveFalseWithFetch(pageable)
      .map(SupplierDTO::fromSupplier);
  }

  /**
   * Retrieves suppliers filtered by currency.
   *
   * @param currency the currency to filter by
   * @param pageable pagination information
   * @return page of supplier DTOs
   */
  @Transactional(readOnly = true)
  public Page<SupplierDTO> getSuppliersByCurrency(
    Currency currency,
    Pageable pageable
  ) {
    return supplierRepository
      .findByCurrencyWithFetch(currency, pageable)
      .map(SupplierDTO::fromSupplier);
  }

  /**
   * Retrieves suppliers filtered by category.
   *
   * @param categoryId the category ID
   * @param pageable   pagination information
   * @return page of supplier DTOs
   */
  @Transactional(readOnly = true)
  public Page<SupplierDTO> getSuppliersByCategory(Long categoryId, Pageable pageable) {
    if (!categoryRepository.existsById(categoryId)) {
      throw new EntityNotFoundException("Supplier category not found: " + categoryId);
    }
    return supplierRepository
      .findByCategoryIdWithFetch(categoryId, pageable)
      .map(SupplierDTO::fromSupplier);
  }

  /**
   * Retrieves supplier performance metrics over a given number of months.
   *
   * @param months the lookback period in months
   * @return list of supplier performance DTOs
   */
  @Transactional(readOnly = true)
  public List<SupplierPerformanceDTO> getSupplierPerformance(int months) {
    LocalDateTime from = LocalDateTime.now()
      .minusMonths(months)
      .withDayOfMonth(1)
      .withHour(0)
      .withMinute(0)
      .withSecond(0);

    List<PurchaseOrderRepository.SupplierPerformanceProjection> rows =
      orderRepository.getSupplierPerformance(from);

    // Agrupar por proveedor
    Map<
      Long,
      List<PurchaseOrderRepository.SupplierPerformanceProjection>
    > bySupplier = rows
      .stream()
      .collect(
        Collectors.groupingBy(
          PurchaseOrderRepository.SupplierPerformanceProjection::getSupplierId
        )
      );

    return bySupplier
      .entrySet()
      .stream()
      .map(entry -> {
        List<
          PurchaseOrderRepository.SupplierPerformanceProjection
        > supplierRows = entry.getValue();

        String supplierName = supplierRows.get(0).getSupplierName();
        long totalOrders = supplierRows
          .stream()
          .mapToLong(r -> r.getTotalOrders())
          .sum();
        long totalDelivered = supplierRows
          .stream()
          .mapToLong(r -> r.getDeliveredOrders())
          .sum();
        BigDecimal totalSpend = supplierRows
          .stream()
          .map(r ->
            r.getTotalSpend() != null ? r.getTotalSpend() : BigDecimal.ZERO
          )
          .reduce(BigDecimal.ZERO, BigDecimal::add);
        double fulfillmentRate =
          totalOrders == 0
            ? 0.0
            : BigDecimal.valueOf((totalDelivered * 100.0) / totalOrders)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        List<SupplierPerformanceDTO.MonthlyEntry> monthlyEntries = supplierRows
          .stream()
          .map(r -> {
            long mo = r.getTotalOrders();
            long md = r.getDeliveredOrders();
            BigDecimal ms =
              r.getTotalSpend() != null ? r.getTotalSpend() : BigDecimal.ZERO;
            double mRate =
              mo == 0
                ? 0.0
                : BigDecimal.valueOf((md * 100.0) / mo)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
            return SupplierPerformanceDTO.MonthlyEntry.builder()
              .month(r.getMonth().toString().substring(0, 7))
              .totalOrders(mo)
              .deliveredOrders(md)
              .totalSpend(ms)
              .fulfillmentRate(mRate)
              .build();
          })
          .toList();

        return SupplierPerformanceDTO.builder()
          .supplierId(entry.getKey())
          .supplierName(supplierName)
          .months(monthlyEntries)
          .totalOrders(totalOrders)
          .totalDelivered(totalDelivered)
          .fulfillmentRate(fulfillmentRate)
          .totalSpend(totalSpend)
          .build();
      })
      .toList();
  }

  /**
   * Retrieves monthly supplier performance data for charting.
   *
   * @param months the lookback period in months
   * @return list of monthly supplier performance DTOs
   */
  @Transactional(readOnly = true)
  public List<SupplierPerformanceMonthlyDTO> getSupplierPerformanceChart(
    int months
  ) {
    LocalDateTime from = LocalDateTime.now()
      .minusMonths(months)
      .withDayOfMonth(1)
      .withHour(0)
      .withMinute(0)
      .withSecond(0);

    List<PurchaseOrderRepository.MonthlySupplierStatsProjection> rows =
      orderRepository.getMonthlySupplierStats(from);

    // Agrupar por mes
    Map<
      String,
      List<PurchaseOrderRepository.MonthlySupplierStatsProjection>
    > byMonth = rows
      .stream()
      .collect(
        Collectors.groupingBy(r -> r.getMonth().toString().substring(0, 7))
      );

    // Por mes: calcular volumen total por proveedor para separar Tier 1 vs Tier 2-3
    // Tier 1 = proveedores en el top 33% por volumen de órdenes globales
    Map<Long, Long> globalVolume = rows
      .stream()
      .collect(
        Collectors.groupingBy(
          PurchaseOrderRepository.MonthlySupplierStatsProjection::getSupplierId,
          Collectors.summingLong(r -> r.getTotalOrders())
        )
      );

    long threshold = globalVolume
      .values()
      .stream()
      .sorted(Comparator.reverseOrder())
      .limit(Math.max(1, globalVolume.size() / 3))
      .min(Comparator.naturalOrder())
      .orElse(1L);

    Set<Long> tier1Suppliers = globalVolume
      .entrySet()
      .stream()
      .filter(e -> e.getValue() >= threshold)
      .map(Map.Entry::getKey)
      .collect(Collectors.toSet());

    return byMonth
      .entrySet()
      .stream()
      .sorted(Map.Entry.comparingByKey())
      .map(entry -> {
        List<PurchaseOrderRepository.MonthlySupplierStatsProjection> monthRows =
          entry.getValue();

        // Tier 1
        double aRate = monthRows
          .stream()
          .filter(r -> tier1Suppliers.contains(r.getSupplierId()))
          .mapToDouble(r ->
            r.getTotalOrders() == 0
              ? 0.0
              : (r.getDeliveredOrders() * 100.0) / r.getTotalOrders()
          )
          .average()
          .orElse(0.0);

        // Tier 2-3
        double bRate = monthRows
          .stream()
          .filter(r -> !tier1Suppliers.contains(r.getSupplierId()))
          .mapToDouble(r ->
            r.getTotalOrders() == 0
              ? 0.0
              : (r.getDeliveredOrders() * 100.0) / r.getTotalOrders()
          )
          .average()
          .orElse(0.0);

        return SupplierPerformanceMonthlyDTO.builder()
          .month(entry.getKey())
          .a(
            BigDecimal.valueOf(aRate)
              .setScale(1, RoundingMode.HALF_UP)
              .doubleValue()
          )
          .b(
            BigDecimal.valueOf(bRate)
              .setScale(1, RoundingMode.HALF_UP)
              .doubleValue()
          )
          .build();
      })
      .toList();
  }
}
