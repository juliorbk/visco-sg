package com.visco.backend.models.dtos;

import java.util.Map;

/**
 * Snapshot of how many rows in each table reference a given user.
 * Returned by GET /api/users/{id}/references so an admin can
 * decide whether a hard delete is safe.
 */
public record UserReferencesResponse(
  long purchaseOrdersCreated,
  long purchaseOrdersApproved,
  long purchaseOrdersRejected,
  long requisitionsRequested,
  long requisitionsApproved,
  long goodReceiptsReceived,
  long dispatchesCreated,
  long inventoryMovementsCreated,
  long warehousesResponsible,
  long inviteTokensCreated
) {
  public long total() {
    return purchaseOrdersCreated +
      purchaseOrdersApproved +
      purchaseOrdersRejected +
      requisitionsRequested +
      requisitionsApproved +
      goodReceiptsReceived +
      dispatchesCreated +
      inventoryMovementsCreated +
      warehousesResponsible +
      inviteTokensCreated;
  }

  public Map<String, Long> asMap() {
    return Map.ofEntries(
      Map.entry("purchaseOrdersCreated", purchaseOrdersCreated),
      Map.entry("purchaseOrdersApproved", purchaseOrdersApproved),
      Map.entry("purchaseOrdersRejected", purchaseOrdersRejected),
      Map.entry("requisitionsRequested", requisitionsRequested),
      Map.entry("requisitionsApproved", requisitionsApproved),
      Map.entry("goodReceiptsReceived", goodReceiptsReceived),
      Map.entry("dispatchesCreated", dispatchesCreated),
      Map.entry("inventoryMovementsCreated", inventoryMovementsCreated),
      Map.entry("warehousesResponsible", warehousesResponsible),
      Map.entry("inviteTokensCreated", inviteTokensCreated)
    );
  }
}
