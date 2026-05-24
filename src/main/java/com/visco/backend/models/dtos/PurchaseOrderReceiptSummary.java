package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PurchaseOrderReceiptSummary {

  private Long orderId;
  private String orderNumber;
  private PurchaseOrderStatus orderStatus;
  private int totalReceipts;
  private List<ItemSummary> items;

  @Data
  @Builder
  public static class ItemSummary {

    private Long productId;
    private String productName;
    private String productSku;
    private BigDecimal orderedQuantity;
    private BigDecimal receivedQuantity;
    private BigDecimal pendingQuantity;
    private boolean fullyReceived;
  }
}
