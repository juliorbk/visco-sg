package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.GoodReceipt;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.models.entities.Warehouse;
import java.time.LocalDateTime;
import java.util.List;

// Response DTO for a goods receipt with items and purchase order summary.
public record GoodReceiptResponse(
    Long id,
    String receiptNumber,
    Long purchaseOrderId,
    String warehousePhysicalAddress,
    String orderNumber,
    PurchaseOrderStatus updatedStatus,
    LocalDateTime receivedAt,
    String notes,
    String receivedBy,
    List<GoodReceiptItemResponse> items,
    PurchaseOrderSummary purchaseOrder
) {
    public record PurchaseOrderSummary(
        SupplierInfo supplier,
        WarehouseInfo destinationWarehouse,
        LocalDateTime createdAt
    ) {
        public static PurchaseOrderSummary fromEntity(PurchaseOrder po) {
            if (po == null) return null;
            return new PurchaseOrderSummary(
                SupplierInfo.fromEntity(po.getSupplier()),
                WarehouseInfo.fromEntity(po.getDestinationWarehouse()),
                po.getCreatedAt()
            );
        }
    }

    public record SupplierInfo(
        String name,
        String address,
        String email,
        List<String> phoneNumbers
    ) {
        public static SupplierInfo fromEntity(Supplier s) {
            if (s == null) return null;
            return new SupplierInfo(
                s.getName(),
                s.getAddress(),
                s.getEmail(),
                s.getPhoneNumbers() == null
                    ? List.of()
                    : List.copyOf(s.getPhoneNumbers())
            );
        }
    }

    public record WarehouseInfo(
        String name,
        String physicalAddress,
        String description,
        String sapCenterCode,
        String responsibleUserName
    ) {
        public static WarehouseInfo fromEntity(Warehouse w) {
            if (w == null) return null;
            return new WarehouseInfo(
                w.getName(),
                w.getPhysicalAddress(),
                w.getDescription(),
                w.getSapCenterCode(),
                w.getResponsibleUser() != null ? w.getResponsibleUser().getName() : null
            );
        }
    }
}
