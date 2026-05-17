package com.visco.backend.controllers;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.visco.backend.models.dtos.PurchaseOrderResponse;
import com.visco.backend.models.entities.PaymentMethod;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.PurchaseOrderType;
import com.visco.backend.repositories.UserRepository;
import com.visco.backend.services.JwtService;
import com.visco.backend.services.ProcurementService;

@WebMvcTest(ProcurementController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProcurementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProcurementService procurementService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private PurchaseOrderResponse createResponse() {
        return new PurchaseOrderResponse(1L, "PO-001", "Test order",
                PurchaseOrderStatus.PENDING, "Test Supplier",
                PaymentMethod.BANK_TRANSFER, PurchaseOrderType.MATERIALS,
                "Test User", LocalDateTime.now(), null, null, null, null, List.of());
    }

    @Test
    @WithMockUser(authorities = "PROCUREMENT")
    void createOrder_shouldReturn201() throws Exception {
        when(procurementService.createPurchaseOrder(any())).thenReturn(createResponse());

        String body = "{\"orderNumber\":\"PO-001\",\"description\":\"Test order\",\"supplierId\":1,"
                + "\"destinationWarehouse\":1,"
                + "\"paymentMethod\":\"BANK_TRANSFER\",\"type\":\"MATERIALS\","
                + "\"createdById\":\"" + UUID.randomUUID() + "\","
                + "\"items\":[{\"productId\":1,\"quantity\":10,\"unitPrice\":100}]}";

        mockMvc.perform(post("/api/procurement/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("PO-001"));
    }

    @Test
    @WithMockUser(authorities = "PROCUREMENT")
    void getAllOrders_shouldReturn200() throws Exception {
        when(procurementService.getAllOrders(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(createResponse())));

        mockMvc.perform(get("/api/procurement/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value("PO-001"));
    }

    @Test
    @WithMockUser(authorities = "PROCUREMENT")
    void getOrder_shouldReturn200() throws Exception {
        when(procurementService.getOrderById(1L)).thenReturn(createResponse());

        mockMvc.perform(get("/api/procurement/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("PO-001"));
    }

    @Test
    @WithMockUser(authorities = "PROCUREMENT")
    void submitForApproval_shouldReturn200() throws Exception {
        when(procurementService.submitOrderForApproval(1L)).thenReturn(new PurchaseOrderResponse(
                1L, "PO-001", "Test", PurchaseOrderStatus.AWAITING_APPROVAL, "Supplier",
                PaymentMethod.BANK_TRANSFER, PurchaseOrderType.MATERIALS, "User",
                LocalDateTime.now(), null, null, null, null, List.of()));

        mockMvc.perform(patch("/api/procurement/orders/1/submit-for-approval"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"));
    }

    @Test
    @WithMockUser(authorities = "PROCUREMENT")
    void markApproved_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        when(procurementService.markAsApproved(eq(1L), any(UUID.class), anyString()))
                .thenReturn(new PurchaseOrderResponse(
                1L, "PO-001", "Test", PurchaseOrderStatus.APPROVED, "Supplier",
                PaymentMethod.BANK_TRANSFER, PurchaseOrderType.MATERIALS, "User",
                LocalDateTime.now(), "Approved", null, "Approver", LocalDateTime.now(), List.of()));

        mockMvc.perform(patch("/api/procurement/orders/1/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + userId + "\",\"notes\":\"Approved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(authorities = "PROCUREMENT")
    void cancelOrder_shouldReturn200() throws Exception {
        when(procurementService.cancelOrder(eq(1L), anyString())).thenReturn(new PurchaseOrderResponse(
                1L, "PO-001", "Test", PurchaseOrderStatus.CANCELLED, "Supplier",
                PaymentMethod.BANK_TRANSFER, PurchaseOrderType.MATERIALS, "User",
                LocalDateTime.now(), null, "Cancelled", null, null, List.of()));

        mockMvc.perform(patch("/api/procurement/orders/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Cancelled\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

}
