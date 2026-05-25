package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.DispatchNote;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
  List<DispatchItemResponse> items
) {
  public static DispatchResponse fromEntity(DispatchNote note) {
    return new DispatchResponse(
      note.getId(),
      note.getDispatchNumber(),
      note.getWarehouse().getName(),
      note.getWithdrawnBy().getFullName(),
      note.getWithdrawnBy().getDocumentNumber(),
      note.getWithdrawnBy().getCostCenter() != null
        ? note.getWithdrawnBy().getCostCenter().getCode()
        : null,
      note.getWithdrawnBy().getCostCenter() != null
        ? note.getWithdrawnBy().getCostCenter().getFullDescription()
        : null,
      note.getCreatedAt(),
      note.getCreatedBy().getName(),
      note.getNotes(),
      note.getItems().stream()
        .map(item -> new DispatchItemResponse(
          item.getProduct().getId(),
          item.getProduct().getName(),
          item.getProduct().getSku(),
          item.getQuantity(),
          item.getExitUnitPrice()
        ))
        .toList()
    );
  }
}
