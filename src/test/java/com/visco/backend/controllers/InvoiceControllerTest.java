package com.visco.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visco.backend.models.dtos.*;
import com.visco.backend.models.entities.InvoiceStatus;
import com.visco.backend.services.InvoiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InvoiceController.class)
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InvoiceService invoiceService;

    @MockitoBean
    private org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration webSecurityConfiguration;

    @Test
    void createInvoice_Success() throws Exception {
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                "INV-001", 1L, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                BigDecimal.valueOf(5000), BigDecimal.valueOf(500), "Notes",
                List.of(new InvoiceItemRequest(1L, BigDecimal.valueOf(100), BigDecimal.valueOf(50), "Notes"))
        );

        InvoiceResponse response = new InvoiceResponse(
                1L, "INV-001", 1L, "PO-001", "Supplier",
                LocalDate.now(), LocalDate.now().plusDays(30),
                BigDecimal.valueOf(5000), BigDecimal.valueOf(500),
                InvoiceStatus.PENDING, null, null, "Notes", LocalDateTime.now(),
                List.of(new InvoiceItemResponse(1L, "Product", "SKU-001", BigDecimal.valueOf(100), BigDecimal.valueOf(50), BigDecimal.valueOf(5000), BigDecimal.valueOf(100), BigDecimal.ZERO, true, true, "Notes"))
        );

        when(invoiceService.createInvoice(any(CreateInvoiceRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"));
    }

    @Test
    void getAllInvoices_ReturnsPage() throws Exception {
        InvoiceResponse invoice = new InvoiceResponse(
                1L, "INV-001", 1L, "PO-001", "Supplier",
                LocalDate.now(), LocalDate.now().plusDays(30),
                BigDecimal.valueOf(5000), BigDecimal.valueOf(500),
                InvoiceStatus.PENDING, null, null, "Notes", LocalDateTime.now(), Collections.emptyList());

        Page<InvoiceResponse> page = new PageImpl<>(List.of(invoice));
        when(invoiceService.getAllInvoices(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/invoices").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-001"));
    }

    @Test
    void getInvoice_Success() throws Exception {
        InvoiceResponse response = new InvoiceResponse(
                1L, "INV-001", 1L, "PO-001", "Supplier",
                LocalDate.now(), LocalDate.now().plusDays(30),
                BigDecimal.valueOf(5000), BigDecimal.valueOf(500),
                InvoiceStatus.PENDING, null, null, "Notes", LocalDateTime.now(), Collections.emptyList());

        when(invoiceService.getInvoiceById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/invoices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"));
    }

    @Test
    void getInvoicesByOrder_Success() throws Exception {
        InvoiceResponse invoice = new InvoiceResponse(
                1L, "INV-001", 1L, "PO-001", "Supplier",
                LocalDate.now(), LocalDate.now().plusDays(30),
                BigDecimal.valueOf(5000), BigDecimal.valueOf(500),
                InvoiceStatus.PENDING, null, null, "Notes", LocalDateTime.now(), Collections.emptyList());

        when(invoiceService.getInvoicesByOrderId(1L)).thenReturn(List.of(invoice));

        mockMvc.perform(get("/api/invoices/by-order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceNumber").value("INV-001"));
    }

    @Test
    void markAsPaid_Success() throws Exception {
        InvoiceResponse response = new InvoiceResponse(
                1L, "INV-001", 1L, "PO-001", "Supplier",
                LocalDate.now(), LocalDate.now().plusDays(30),
                BigDecimal.valueOf(5000), BigDecimal.valueOf(500),
                InvoiceStatus.PAID, null, LocalDate.now(), "Notes", LocalDateTime.now(), Collections.emptyList());

        when(invoiceService.markAsPaid(eq(1L), any(LocalDate.class))).thenReturn(response);

        mockMvc.perform(patch("/api/invoices/1/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentDate\":\"2025-01-15\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void cancelInvoice_Success() throws Exception {
        InvoiceResponse response = new InvoiceResponse(
                1L, "INV-001", 1L, "PO-001", "Supplier",
                LocalDate.now(), LocalDate.now().plusDays(30),
                BigDecimal.valueOf(5000), BigDecimal.valueOf(500),
                InvoiceStatus.CANCELLED, null, null, "Notes", LocalDateTime.now(), Collections.emptyList());

        when(invoiceService.cancelInvoice(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/invoices/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
