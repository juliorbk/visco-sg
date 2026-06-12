package com.visco.backend.repositories;

import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
// Repository for user accounts with email lookups, role queries, and hard-delete helpers.
public interface UserRepository extends JpaRepository<User, UUID> {
    // Finds a user by their exact email address.
    Optional<User> findByEmail(String email);

    @Query(
      "SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)"
    )
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);

    @Query(
      "SELECT u FROM User u LEFT JOIN FETCH u.costCenter " +
      "WHERE LOWER(u.email) = LOWER(:email)"
    )
    Optional<User> findByEmailWithCostCenter(@Param("email") String email);

    @Query(
        value = "SELECT u FROM User u LEFT JOIN FETCH u.costCenter",
        countQuery = "SELECT COUNT(u) FROM User u"
    )
    Page<User> findAllWithFetch(Pageable pageable);

    @Query("SELECT u.role as role, COUNT(u) as count FROM User u GROUP BY u.role")
    List<UserRoleCountProjection> countByRole();

    @Query("SELECT u.email FROM User u WHERE u.active = true AND u.role IN ('ADMIN', 'MANAGER')")
    List<String> findActiveAdminAndManagerEmails();

    // Counts users by role regardless of active status.
    long countByRole(UserRole role);

    // Counts active users by role.
    long countByRoleAndActiveTrue(UserRole role);

    // Raw lookup without any fetch-joins, bypassing auditing.
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdRaw(@Param("id") UUID id);

    // ── Hard-delete helpers: null out FK columns referencing a user
    // before deleting the user itself. Each @Modifying clears one
    // specific relationship. Kept here (rather than in a separate
    // repository) so the hard-delete flow is self-contained.

    @Modifying
    @Query(
      "UPDATE PurchaseOrder p SET p.createdBy = null WHERE p.createdBy.id = :userId"
    )
    int nullifyCreatedByInPurchaseOrders(@Param("userId") UUID userId);

    @Modifying
    @Query(
      "UPDATE PurchaseOrder p SET p.approvedBy = null WHERE p.approvedBy.id = :userId"
    )
    int nullifyApprovedByInPurchaseOrders(@Param("userId") UUID userId);

    @Modifying
    @Query(
      "UPDATE PurchaseOrder p SET p.rejectedBy = null WHERE p.rejectedBy.id = :userId"
    )
    int nullifyRejectedByInPurchaseOrders(@Param("userId") UUID userId);

    @Modifying
    @Query(
      "UPDATE Requisition r SET r.requestedBy = null WHERE r.requestedBy.id = :userId"
    )
    int nullifyRequestedByInRequisitions(@Param("userId") UUID userId);

    @Modifying
    @Query(
      "UPDATE Requisition r SET r.approvedBy = null WHERE r.approvedBy.id = :userId"
    )
    int nullifyApprovedByInRequisitions(@Param("userId") UUID userId);

    @Modifying
    @Query(
      "UPDATE GoodReceipt g SET g.receivedBy = null WHERE g.receivedBy.id = :userId"
    )
    int nullifyReceivedByInGoodReceipts(@Param("userId") UUID userId);

    @Modifying
    @Query(
      "UPDATE DispatchNote d SET d.createdBy = null WHERE d.createdBy.id = :userId"
    )
    int nullifyCreatedByInDispatchNotes(@Param("userId") UUID userId);

    @Modifying
    @Query(
      "UPDATE InventoryMovement i SET i.createdBy = null WHERE i.createdBy.id = :userId"
    )
    int nullifyCreatedByInInventoryMovements(@Param("userId") UUID userId);

    @Modifying
    @Query(
      "UPDATE Warehouse w SET w.responsibleUser = null WHERE w.responsibleUser.id = :userId"
    )
    int nullifyResponsibleUserInWarehouses(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM InviteToken t WHERE t.createdById = :userId")
    int deleteInviteTokensByCreator(@Param("userId") UUID userId);

    interface UserRoleCountProjection {
        UserRole getRole();

        Long getCount();
    }
}
