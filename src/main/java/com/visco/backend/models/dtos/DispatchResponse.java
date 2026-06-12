package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.DispatchNote;
import com.visco.backend.models.entities.Warehouse;
import java.time.LocalDateTime;
import java.util.List;

// Response DTO for a complete dispatch note with items and warehouse info.
public record DispatchResponse(
    Long id,
    String dispatchNumber,
    String warehouseName,
    String employeeName,
    String employeeDocument,
    String costCenterCode,
    String costCenterDescription,
    LocalDateTime createdAt,
    String createdByName,
    String notes,
    List<DispatchItemResponse> items,
    WarehouseInfo warehouse
) {
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

    public static DispatchResponse fromEntity(DispatchNote note) {
        return new DispatchResponse(
            note.getId(),
            note.getDispatchNumber(),
            note.getWarehouse().getName(),
            note.getWithdrawnBy() != null ? note.getWithdrawnBy().getFullName() : null,
            note.getWithdrawnBy() != null ? note.getWithdrawnBy().getDocumentNumber() : null,
            note.getWithdrawnBy() != null && note.getWithdrawnBy().getCostCenter() != null
                ? note.getWithdrawnBy().getCostCenter().getCode()
                : null,
            note.getWithdrawnBy() != null && note.getWithdrawnBy().getCostCenter() != null
                ? note.getWithdrawnBy().getCostCenter().getFullDescription()
                : null,
            note.getCreatedAt(),
            note.getCreatedBy() != null ? note.getCreatedBy().getName() : null,
            note.getNotes(),
            note
                .getItems()
                .stream()
                .map((item) ->
                    new DispatchItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getProduct().getSku(),
                        item.getProduct().getUom().name(),
                        item.getQuantity()
                    )
                )
                .toList(),
            WarehouseInfo.fromEntity(note.getWarehouse())
        );
    }
}
