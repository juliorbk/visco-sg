package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecentOrderDTO {

  private Long id;
  private String orderNumber;
  private LocalDateTime createdAt;
  private String supplierName;
  private PurchaseOrderStatus status;
  private BigDecimal amount;
}
