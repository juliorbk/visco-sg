package com.visco.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visco.backend.models.dtos.*;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.PurchaseOrderType;
import com.visco.backend.services.ProcurementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProcurementController.class)
class ProcurementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProcurementService procurementService;

    @MockitoBean
    private org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration webSecurityConfiguration;

    @Test
    void createOrder_Success() throws Exception {
        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest(
                "PO-001", "Description", 1L, 1L, 1L, "NET30",
                PurchaseOrderType.MATERIALS, 7, null,
                List.of(new PurchaseOrderItemRequest(1L, 100, BigDecimal.valueOf(50)))
        );

        PurchaseOrderResponse response = new PurchaseOrderResponse(
                1L, "PO-001", "Description", PurchaseOrderStatus.PENDING,
                "Supplier", "NET30", PurchaseOrderType.MATERIALS, "User",
                LocalDateTime.now(), null, null, null, null, null, null, null, 7,
                List.of(new PurchaseOrderItemResponse(1L, "Product", "SKU-001", 100, BigDecimal.valueOf(50), BigDecimal.valueOf(5000)))
        );

        when(procurementService.createPurchaseOrder(any(CreatePurchaseOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/procurement/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("PO-001"));
    }

    @Test
    void getAllOrders_ReturnsPage() throws Exception {
        PurchaseOrderResponse order = new PurchaseOrderResponse(
                1L, "PO-001", "Description", PurchaseOrderStatus.PENDING,
                "Supplier", "NET30", PurchaseOrderType.MATERIALS, "User",
                LocalDateTime.now(), null, null, null, null, null, null, null, 7, Collections.emptyList());

        Page<PurchaseOrderResponse> page = new PageImpl<>(List.of(order));
        when(procurementService.getAllOrders(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/procurement/orders").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value("PO-001"));
    }

    @Test
    void getOrder_Success() throws Exception {
        PurchaseOrderResponse response = new PurchaseOrderResponse(
                1L, "PO-001", "Description", PurchaseOrderStatus.PENDING,
                "Supplier", "NET30", PurchaseOrderType.MATERIALS, "User",
                LocalDateTime.now(), null, null, null, null, null, null, null, 7, Collections.emptyList());

        when(procurementService.getOrderById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/procurement/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("PO-001"));
    }

    @Test
    void submitForApproval_Success() throws Exception {
        PurchaseOrderResponse response = new PurchaseOrderResponse(
                1L, "PO-001", "Description", PurchaseOrderStatus.AWAITING_APPROVAL,
                "Supplier", "NET30", PurchaseOrderType.MATERIALS, "User",
                LocalDateTime.now(), null, null, null, null, null, null, null, 7, Collections.emptyList());

        when(procurementService.submitOrderForApproval(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/procurement/orders/1/submit-for-approval"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"));
    }

    @Test
    void markApproved_Success() throws Exception {
        PurchaseOrderResponse response = new PurchaseOrderResponse(
                1L, "PO-001", "Description", PurchaseOrderStatus.APPROVED,
                "Supplier", "NET30", PurchaseOrderType.MATERIALS, "User",
                LocalDateTime.now(), null, null, null, null, null, null, null, 7, Collections.emptyList());

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("admin@visco.com");
        when(procurementService.markAsApprovedByEmail(eq(1L), eq("admin@visco.com"), any())).thenReturn(response);

        mockMvc.perform(patch("/api/procurement/orders/1/approve")
                        .principal(userDetails)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Approved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void rejectOrder_Success() throws Exception {
        PurchaseOrderResponse response = new PurchaseOrderResponse(
                1L, "PO-001", "Description", PurchaseOrderStatus.REJECTED,
                "Supplier", "NET30", PurchaseOrderType.MATERIALS, "User",
                LocalDateTime.now(), null, null, null, null, null, null, null, 7, Collections.emptyList());

        when(procurementService.rejectPurchaseOrder(eq(1L), any(UUID.class), eq("Reason"))).thenReturn(response);

        mockMvc.perform(patch("/api/procurement/orders/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"550e8400-e29b-41d4-a716-446655440000\",\"reason\":\"Reason\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void sendToSupplier_Success() throws Exception {
        PurchaseOrderResponse response = new PurchaseOrderResponse(
                1L, "PO-001", "Description", PurchaseOrderStatus.IN_TRANSIT,
                "Supplier", "NET30", PurchaseOrderType.MATERIALS, "User",
                LocalDateTime.now(), null, null, null, null, null, null, null, 7, Collections.emptyList());

        when(procurementService.markAsSentToSupplier(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/procurement/orders/1/send-to-supplier"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));
    }

    @Test
    void cancelOrder_Success() throws Exception {
        PurchaseOrderResponse response = new PurchaseOrderResponse(
                1L, "PO-001", "Description", PurchaseOrderStatus.CANCELLED,
                "Supplier", "NET30", PurchaseOrderType.MATERIALS, "User",
                LocalDateTime.now(), null, null, null, null, null, null, null, 7, Collections.emptyList());

        when(procurementService.cancelOrder(eq(1L), eq("Cancelled"))).thenReturn(response);

        mockMvc.perform(patch("/api/procurement/orders/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Cancelled\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
