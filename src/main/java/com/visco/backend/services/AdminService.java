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
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Handles business logic for administrative user management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final CostCenterRepository costCenterRepository;
    private final UserReferenceCountRepository userReferenceCountRepository;
    private final Cloudinary cloudinary;

    /**
     * Retrieves a paginated list of all users.
     *
     * @param pageable pagination information
     * @return page of user DTOs
     */
    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAllWithFetch(pageable).map(UserDTO::fromUser);
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param id the user UUID
     * @return the user DTO
     */
    @Transactional(readOnly = true)
    public UserDTO getUserById(UUID id) {
        User user = userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        return UserDTO.fromUser(user);
    }

    /**
     * Updates a user's role and cost center.
     *
     * @param id         the user UUID
     * @param request    the update request
     * @param callerRole the role of the calling user
     * @return the updated user DTO
     */
    @Transactional
    public UserDTO updateUser(
        UUID id,
        UpdateUserRequest request,
        UserRole callerRole
    ) {
        User user = userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

        // Privilege escalation guard: only SUPERADMIN can grant SUPERADMIN.
        if (
            request.role() == UserRole.SUPERADMIN &&
            callerRole != UserRole.SUPERADMIN
        ) {
            throw new IllegalStateException(
                "Only a SUPERADMIN can assign the SUPERADMIN role"
            );
        }

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

    /**
     * Deactivates a user account (soft disable).
     *
     * @param id the user UUID
     */
    @Transactional
    public void deactivateUser(UUID id) {
        User user = userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        user.setActive(false);
        userRepository.save(user);
    }

    /**
     * Permanently deletes a user from the database.
     *
     * @param id the user UUID
     */
    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        userRepository.delete(user);
    }

    /**
     * Activates a previously deactivated user account.
     *
     * @param id the user UUID
     */
    @Transactional
    public void activateUser(UUID id) {
        User user = userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        user.setActive(true);
        userRepository.save(user);
    }

    /**
     * Counts all database references to a user across related entities.
     *
     * @param userId the user UUID
     * @return reference counts grouped by entity type
     */
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

        // Last-active-SUPERADMIN guard, enforced inside the same transaction
        // as the delete to avoid the TOCTOU race that the controller-level
        // check suffered from. Inactive SUPERADMINs are not counted.
        if (user.getRole() == UserRole.SUPERADMIN && Boolean.TRUE.equals(user.getActive())) {
            long activeSuperadmins = userRepository.countByRoleAndActiveTrue(UserRole.SUPERADMIN);
            if (activeSuperadmins <= 1) {
                throw new IllegalStateException(
                    "Refusing to delete the last active SUPERADMIN. Promote another user " +
                        "to SUPERADMIN before deleting this one."
                );
            }
        }

        UserReferencesResponse refs = countUserReferences(id);
        if (refs.total() > 0 && !force) {
            throw new IllegalStateException(
                "User cannot be hard-deleted: " +
                    refs.total() +
                    " dependent row(s) reference this user. " +
                    "Use ?force=true to nullify references and proceed."
            );
        }

        // Force path: only run the nullify updates if there is at least
        // one dependent row, to avoid touching unrelated tables.
        if (force && refs.total() > 0) {
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
        return userRepository.countByRoleAndActiveTrue(UserRole.SUPERADMIN);
    }

    @Transactional
    public UserDTO uploadProfilePicture(UUID userId, MultipartFile file) {
        User user = userRepository
            .findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                file.getInputStream(),
                ObjectUtils.asMap(
                    "folder", "profile-pictures",
                    "public_id", "user_" + userId,
                    "overwrite", true,
                    "resource_type", "image"
                )
            );
            String url = (String) uploadResult.get("secure_url");
            user.setProfilePictureUrl(url);
            userRepository.save(user);
            log.info("Profile picture uploaded for user {}", userId);
            return UserDTO.fromUser(user);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload profile picture", e);
        }
    }

    @Transactional
    public UserDTO deleteProfilePicture(UUID userId) {
        User user = userRepository
            .findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        try {
            cloudinary.uploader().destroy(
                "profile-pictures/user_" + userId,
                ObjectUtils.asMap("resource_type", "image")
            );
        } catch (IOException e) {
            log.warn("Failed to delete profile picture from Cloudinary: {}", e.getMessage());
        }

        user.setProfilePictureUrl(null);
        userRepository.save(user);
        log.info("Profile picture removed for user {}", userId);
        return UserDTO.fromUser(user);
    }
}
