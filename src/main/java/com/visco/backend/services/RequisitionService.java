package com.visco.backend.services;

import com.visco.backend.models.dtos.CreateRequisitionRequest;
import com.visco.backend.models.dtos.RequisitionItemRequest;
import com.visco.backend.models.dtos.RequisitionItemResponse;
import com.visco.backend.models.dtos.RequisitionResponse;
import com.visco.backend.models.dtos.UpdateRequisition;
import com.visco.backend.models.entities.CostCenter;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.Requisition;
import com.visco.backend.models.entities.RequisitionItem;
import com.visco.backend.models.entities.RequisitionStatus;
import com.visco.backend.models.entities.User;
import com.visco.backend.repositories.CostCenterRepository;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.RequisitionRepository;
import com.visco.backend.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequisitionService {

  private final RequisitionRepository requisitionRepository;
  private final UserRepository userRepository;
  private final CostCenterRepository costCenterRepository;
  private final ProductRepository productRepository;

  @Transactional
  public RequisitionResponse createRequisition(
    CreateRequisitionRequest request
  ) {
    User requestedBy = userRepository
      .findById(request.requestedById())
      .orElseThrow(() ->
        new EntityNotFoundException(
          "User not found: " + request.requestedById()
        )
      );

    CostCenter costCenter = costCenterRepository
      .findById(request.costCenterId())
      .orElseThrow(() ->
        new EntityNotFoundException("Area not found: " + request.costCenterId())
      );

    Requisition requisition = Requisition.builder()
      .requisitionNumber(request.requisitionNumber())
      .description(request.description())
      .requestedBy(requestedBy)
      .costCenter(costCenter)
      .status(RequisitionStatus.PENDING)
      .createdAt(LocalDateTime.now())
      .build();

    Map<Long, Product> productMap = productRepository
      .findAllById(
        request.items().stream().map(RequisitionItemRequest::productId).toList()
      )
      .stream()
      .collect(Collectors.toMap(Product::getId, p -> p));

    for (RequisitionItemRequest itemReq : request.items()) {
      Product product = productMap.get(itemReq.productId());
      if (product == null) {
        throw new EntityNotFoundException(
          "Product not found: " + itemReq.productId()
        );
      }
      RequisitionItem item = RequisitionItem.builder()
        .requisition(requisition)
        .product(product)
        .quantity(itemReq.quantity())
        .notes(itemReq.notes())
        .build();
      requisition.getItems().add(item);
    }

    Requisition saved = requisitionRepository.save(requisition);
    log.info(
      "Created requisition: {} by user: {}",
      saved.getRequisitionNumber(),
      requestedBy.getName()
    );
    return toResponse(saved);
  }

  @Transactional
  public RequisitionResponse submitForApproval(Long id) {
    Requisition req = findById(id);
    if (
      req.getStatus() != RequisitionStatus.PENDING &&
      req.getStatus() != RequisitionStatus.DRAFT
    ) {
      throw new IllegalStateException(
        "Only PENDING or DRAFT requisitions can be submitted for approval"
      );
    }
    req.setStatus(RequisitionStatus.AWAITING_APPROVAL);
    Requisition saved = requisitionRepository.save(req);
    log.info(
      "Requisition {} submitted for approval",
      saved.getRequisitionNumber()
    );
    return toResponse(saved);
  }

  @Transactional
  public RequisitionResponse approveRequisition(
    Long id,
    UUID approverUserId,
    String notes
  ) {
    Requisition req = findById(id);
    if (req.getStatus() != RequisitionStatus.AWAITING_APPROVAL) {
      throw new IllegalStateException(
        "Only requisitions awaiting approval can be approved"
      );
    }
    User approver = userRepository
      .findById(approverUserId)
      .orElseThrow(() ->
        new EntityNotFoundException("User not found: " + approverUserId)
      );
    req.setStatus(RequisitionStatus.APPROVED);
    req.setApprovedBy(approver);
    req.setApprovedAt(LocalDateTime.now());
    req.setApprovalNotes(notes);
    Requisition saved = requisitionRepository.save(req);
    log.info(
      "Requisition {} approved by: {}",
      saved.getRequisitionNumber(),
      approver.getName()
    );
    return toResponse(saved);
  }

  @Transactional
  public RequisitionResponse rejectRequisition(
    Long id,
    UUID rejecterUserId,
    String reason
  ) {
    Requisition req = findById(id);
    if (req.getStatus() != RequisitionStatus.AWAITING_APPROVAL) {
      throw new IllegalStateException(
        "Only requisitions awaiting approval can be rejected"
      );
    }
    req.setStatus(RequisitionStatus.REJECTED);
    req.setRejectionReason(reason);
    if (rejecterUserId != null) {
      userRepository.findById(rejecterUserId).ifPresent(req::setApprovedBy);
    }
    Requisition saved = requisitionRepository.save(req);
    log.info(
      "Requisition {} rejected. Reason: {}",
      saved.getRequisitionNumber(),
      reason
    );
    return toResponse(saved);
  }

  @Transactional
  public RequisitionResponse cancelRequisition(Long id) {
    Requisition req = findById(id);
    if (
      req.getStatus() == RequisitionStatus.CONVERTED ||
      req.getStatus() == RequisitionStatus.CANCELLED
    ) {
      throw new IllegalStateException(
        "Cannot cancel a " + req.getStatus() + " requisition"
      );
    }
    req.setStatus(RequisitionStatus.CANCELLED);
    Requisition saved = requisitionRepository.save(req);
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public Page<RequisitionResponse> getAllRequisitions(Pageable pageable) {
    return requisitionRepository.findAll(pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public Page<RequisitionResponse> getRequisitionsByStatus(
    RequisitionStatus status,
    Pageable pageable
  ) {
    return requisitionRepository
      .findByStatus(status, pageable)
      .map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public RequisitionResponse getRequisitionById(Long id) {
    return toResponse(findById(id));
  }

  private Requisition findById(Long id) {
    return requisitionRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Requisition not found: " + id)
      );
  }

  @Transactional
  public RequisitionResponse markAsConverted(Long id) {
    Requisition req = findById(id);
    if (req.getStatus() != RequisitionStatus.APPROVED) {
      throw new IllegalStateException(
        "Only approved requisitions can be converted to PO"
      );
    }
    req.setStatus(RequisitionStatus.CONVERTED);
    Requisition saved = requisitionRepository.save(req);
    return toResponse(saved);
  }

  @Transactional
  public RequisitionResponse updateRequisition(
    Long id,
    UUID requestedById,
    UpdateRequisition request
  ) {
    Requisition req = findById(id);
    User requestedBy = userRepository
      .findById(requestedById)
      .orElseThrow(() ->
        new EntityNotFoundException("Requester not found: " + requestedById)
      );
    if (req.getStatus() != RequisitionStatus.DRAFT) {
      throw new IllegalStateException("Only DRAFT requisitions can be updated");
    }

    req.setDescription(request.description());

    if (!req.getCostCenter().getId().equals(request.costCenterId())) {
      CostCenter costCenter = costCenterRepository
        .findById(request.costCenterId())
        .orElseThrow(() ->
          new EntityNotFoundException(
            "Area not found: " + request.costCenterId()
          )
        );
      req.setCostCenter(costCenter);
    }

    req.getItems().clear();

    Map<Long, Product> productMap = productRepository
      .findAllById(
        request.items().stream().map(RequisitionItemRequest::productId).toList()
      )
      .stream()
      .collect(Collectors.toMap(Product::getId, p -> p));

    for (RequisitionItemRequest itemReq : request.items()) {
      Product product = productMap.get(itemReq.productId());
      if (product == null) {
        throw new EntityNotFoundException(
          "Product not found: " + itemReq.productId()
        );
      }
      RequisitionItem item = RequisitionItem.builder()
        .requisition(req)
        .product(product)
        .quantity(itemReq.quantity())
        .notes(itemReq.notes())
        .build();
      req.getItems().add(item);
    }

    req.setUpdatedAt(LocalDateTime.now());
    Requisition saved = requisitionRepository.save(req);
    log.info(
      "Updated requisition: {} by user: {}",
      saved.getRequisitionNumber(),
      requestedBy.getName()
    );
    return toResponse(saved);
  }

  private RequisitionResponse toResponse(Requisition req) {
    List<RequisitionItemResponse> itemResponses = req
      .getItems()
      .stream()
      .map(item ->
        new RequisitionItemResponse(
          item.getProduct().getId(),
          item.getProduct().getName(),
          item.getProduct().getSku(),
          item.getQuantity(),
          item.getNotes()
        )
      )
      .toList();

    return new RequisitionResponse(
      req.getId(),
      req.getRequisitionNumber(),
      req.getDescription(),
      req.getRequestedBy().getName(),
      req.getCostCenter().getFullDescription(),
      req.getStatus(),
      req.getRejectionReason(),
      req.getApprovalNotes(),
      req.getApprovedBy() != null ? req.getApprovedBy().getName() : null,
      req.getApprovedAt(),
      req.getCreatedAt(),
      itemResponses
    );
  }
}
