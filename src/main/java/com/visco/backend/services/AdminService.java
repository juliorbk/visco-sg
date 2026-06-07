package com.visco.backend.services;

import com.visco.backend.models.dtos.UpdateUserRequest;
import com.visco.backend.models.dtos.UserDTO;
import com.visco.backend.models.dtos.UserReferencesResponse;
import com.visco.backend.models.entities.CostCenter;
import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.UserRole;
import com.visco.backend.repositories.CostCenterRepository;
import com.visco.backend.repositories.UserReferenceCountRepository;
import com.visco.backend.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final CostCenterRepository costCenterRepository;
    private final UserReferenceCountRepository userReferenceCountRepository;

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAllWithFetch(pageable).map(UserDTO::fromUser);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(UUID id) {
        User user = userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        return UserDTO.fromUser(user);
    }

    @Transactional
    public UserDTO updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

        user.setRole(request.role());

        if (request.costCenterId() != null) {
            CostCenter costCenter = costCenterRepository
                .findById(request.costCenterId())
                .orElseThrow(() ->
                    new EntityNotFoundException("Area not found: " + request.costCenterId())
                );
            user.setCostCenter(costCenter);
        } else {
            user.setCostCenter(null);
        }

        return UserDTO.fromUser(userRepository.save(user));
    }

    @Transactional
    public void deactivateUser(UUID id) {
        User user = userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        userRepository.delete(user);
        userRepository.save(user);
    }

    @Transactional
    public void activateUser(UUID id) {
        User user = userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        user.setActive(true);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserReferencesResponse countUserReferences(UUID userId) {
        // Touch the row to fail fast with a clear 404 if the user does not exist.
        userRepository
            .findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        return new UserReferencesResponse(
            userReferenceCountRepository.countPurchaseOrdersCreated(userId),
            userReferenceCountRepository.countPurchaseOrdersApproved(userId),
            userReferenceCountRepository.countPurchaseOrdersRejected(userId),
            userReferenceCountRepository.countRequisitionsRequested(userId),
            userReferenceCountRepository.countRequisitionsApproved(userId),
            userReferenceCountRepository.countGoodReceiptsReceived(userId),
            userReferenceCountRepository.countDispatchesCreated(userId),
            userReferenceCountRepository.countInventoryMovementsCreated(userId),
            userReferenceCountRepository.countWarehousesResponsible(userId),
            userReferenceCountRepository.countInviteTokensCreated(userId)
        );
    }

    /**
     * Hard-delete a user. Only User rows can be hard-deleted; every
     * other domain entity uses @SQLDelete for soft delete.
     *
     * The caller MUST be SUPERADMIN; the authorization is enforced
     * at the controller layer (@PreAuthorize).
     *
     * Refuses the operation if the user is still referenced anywhere
     * in the database, unless the explicit `force` flag is set, in
     * which case the references are nullified in dependent rows first.
     */
    @Transactional
    public void hardDeleteUser(UUID id, boolean force) {
        User user = userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

        UserReferencesResponse refs = countUserReferences(id);
        if (refs.total() > 0 && !force) {
            throw new IllegalStateException(
                "User cannot be hard-deleted: " +
                    refs.total() +
                    " dependent row(s) reference this user. " +
                    "Use ?force=true to nullify references and proceed."
            );
        }

        // Force path: clear the FK columns referencing this user.
        // We do this even when refs.total() == 0 because the user
        // might be a SUPERADMIN being removed from a small system.
        if (force) {
            nullifyUserReferences(id);
        }

        userRepository.delete(user);
    }

    @Transactional
    protected void nullifyUserReferences(UUID userId) {
        // Native updates are the simplest way to null out a FK column
        // without having to load every dependent row.
        userRepository.nullifyCreatedByInPurchaseOrders(userId);
        userRepository.nullifyApprovedByInPurchaseOrders(userId);
        userRepository.nullifyRejectedByInPurchaseOrders(userId);
        userRepository.nullifyRequestedByInRequisitions(userId);
        userRepository.nullifyApprovedByInRequisitions(userId);
        userRepository.nullifyReceivedByInGoodReceipts(userId);
        userRepository.nullifyCreatedByInDispatchNotes(userId);
        userRepository.nullifyCreatedByInInventoryMovements(userId);
        userRepository.nullifyResponsibleUserInWarehouses(userId);
        userRepository.deleteInviteTokensByCreator(userId);
    }

    /**
     * Defensive helper: a SUPERADMIN should never be the last
     * SUPERADMIN in the system, otherwise the system could end
     * up with no admin and locked out of recovery operations.
     */
    @Transactional(readOnly = true)
    public long countSuperadmins() {
        return userRepository.countByRole(UserRole.SUPERADMIN);
    }
}
