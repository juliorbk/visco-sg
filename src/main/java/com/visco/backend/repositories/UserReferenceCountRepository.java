package com.visco.backend.repositories;

import com.visco.backend.models.entities.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Reference counters for a User. Used by the SUPERADMIN-only
 * hard-delete flow to decide whether a user can be safely removed.
 *
 * Each query returns the number of rows in the corresponding
 * table that reference the given user. The numbers are not
 * expected to be huge, so a separate query per table is fine
 * and keeps the SQL portable across PostgreSQL and H2.
 */
@Repository
public interface UserReferenceCountRepository extends JpaRepository<User, UUID> {

  @Query(
    "SELECT COUNT(p) FROM PurchaseOrder p WHERE p.createdBy.id = :userId"
  )
  long countPurchaseOrdersCreated(@Param("userId") UUID userId);

  @Query(
    "SELECT COUNT(p) FROM PurchaseOrder p WHERE p.approvedBy.id = :userId"
  )
  long countPurchaseOrdersApproved(@Param("userId") UUID userId);

  @Query(
    "SELECT COUNT(p) FROM PurchaseOrder p WHERE p.rejectedBy.id = :userId"
  )
  long countPurchaseOrdersRejected(@Param("userId") UUID userId);

  @Query(
    "SELECT COUNT(r) FROM Requisition r WHERE r.requestedBy.id = :userId"
  )
  long countRequisitionsRequested(@Param("userId") UUID userId);

  @Query(
    "SELECT COUNT(r) FROM Requisition r WHERE r.approvedBy.id = :userId"
  )
  long countRequisitionsApproved(@Param("userId") UUID userId);

  @Query(
    "SELECT COUNT(g) FROM GoodReceipt g WHERE g.receivedBy.id = :userId"
  )
  long countGoodReceiptsReceived(@Param("userId") UUID userId);

  @Query(
    "SELECT COUNT(d) FROM DispatchNote d WHERE d.createdBy.id = :userId"
  )
  long countDispatchesCreated(@Param("userId") UUID userId);

  @Query(
    "SELECT COUNT(i) FROM InventoryMovement i WHERE i.createdBy.id = :userId"
  )
  long countInventoryMovementsCreated(@Param("userId") UUID userId);

  @Query(
    "SELECT COUNT(w) FROM Warehouse w WHERE w.responsibleUser.id = :userId"
  )
  long countWarehousesResponsible(@Param("userId") UUID userId);

  @Query(
    "SELECT COUNT(t) FROM InviteToken t WHERE t.createdById = :userId"
  )
  long countInviteTokensCreated(@Param("userId") UUID userId);
}
