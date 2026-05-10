package com.visco.backend.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.visco.backend.models.dtos.CreatePurchaseOrderRequest;
import com.visco.backend.models.dtos.PurchaseOrderItemRequest;
import com.visco.backend.models.dtos.PurchaseOrderResponse;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderItem;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.PurchaseOrderRepository;
import com.visco.backend.repositories.StockLevelRepository;
import com.visco.backend.repositories.SupplierRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProcurementService {
    private final ProductService productService;
    private final SupplierService supplierService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StockLevelRepository stockLevelRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    @Transactional
    private PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderRequest request) {

        // Supplier validation
        Supplier supplier = supplierRepository.findById(request.supplierId()).orElseThrow(
                () -> new EntityNotFoundException("Supplier not found" + request.supplierId()));

        // Build order

        PurchaseOrder order = PurchaseOrder.builder().orderNumber(request.orderNumber())
                .description(request.description()).status(PurchaseOrderStatus.PENDING)
                .supplier(supplier).createdAt(LocalDateTime.now()).build();

        for (PurchaseOrderItemRequest itemReq : request.items()) {

            Product product = productRepository.findById(itemReq.productId()).orElseThrow(
                    () -> new EntityNotFoundException("Product not found: " + itemReq.productId()));

            PurchaseOrderItem item =
                    PurchaseOrderItem.builder().purchaseOrder(order).product(product)
                            .quantity(itemReq.quantity()).unitPrice(itemReq.unitPrice()).build();

            order.getItems().add(item);

            addPendingStock(product.getId(), BigDecimal.valueOf(itemReq.quantity()));

            return toResponse(saved);
        }
    }
}
