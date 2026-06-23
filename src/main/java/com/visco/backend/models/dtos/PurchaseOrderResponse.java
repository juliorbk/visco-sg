package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.Incoterm;
import com.visco.backend.models.entities.PaymentMethod;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.PurchaseOrderType;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.models.entities.Warehouse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Response DTO for a complete purchase order with items and related parties.
public record PurchaseOrderResponse(
    Long id,
    String orderNumber,
    String description,
    PurchaseOrderStatus status,
    String supplierName,
    String supplierRif,
    PaymentMethod paymentMethod,
    PurchaseOrderType type,
    String requisitionNumber,
    String shipConditions,
    Incoterm incoterm,
    String createdBy,
    LocalDateTime createdAt,
    String approvalNotes,
    String rejectionReason,
    String approvedBy,
    LocalDateTime approvedAt,
    Long requisitionId,
    Long destinationWarehouseId,
    String destinationWarehouseName,
    Integer leadTime,
    BigDecimal subtotal,
    BigDecimal taxAmount,
    BigDecimal totalAmount,
    SupplierInfo supplier,
    WarehouseInfo destinationWarehouse,
    List<PurchaseOrderItemResponse> items
) {
    public record SupplierInfo(
        String name,
        String address,
        String email,
        String taxId,
        List<String> phoneNumbers
    ) {
        public static SupplierInfo fromEntity(Supplier s) {
            if (s == null) return null;
            return new SupplierInfo(
                s.getName(),
                s.getAddress(),
                s.getEmail(),
                s.getTaxId(),
                s.getPhoneNumbers() == null ? List.of() : List.copyOf(s.getPhoneNumbers())
            );
        }
    }

    public record WarehouseInfo(
        String name,
        String physicalAddress,
        String description,
        String sapCenterCode,
        String responsibleUserName,
        String responsibleUserEmail
    ) {
        public static WarehouseInfo fromEntity(Warehouse w) {
            if (w == null) return null;
            return new WarehouseInfo(
                w.getName(),
                w.getPhysicalAddress(),
                w.getDescription(),
                w.getSapCenterCode(),
                w.getResponsibleUser() != null ? w.getResponsibleUser().getName() : null,
                w.getResponsibleUser() != null ? w.getResponsibleUser().getEmail() : null
            );
        }
    }
}
