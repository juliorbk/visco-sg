package com.visco.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visco.backend.models.dtos.*;
import com.visco.backend.models.entities.RequisitionStatus;
import com.visco.backend.services.RequisitionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RequisitionController.class)
class RequisitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RequisitionService requisitionService;

    @MockitoBean
    private org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration webSecurityConfiguration;

    @Test
    void createRequisition_Success() throws Exception {
        CreateRequisitionRequest request = new CreateRequisitionRequest(
                "REQ-001", "Description", 1L, 1L,
                List.of(new RequisitionItemRequest(1L, 10, "Notes"))
        );

        RequisitionResponse response = new RequisitionResponse(
                1L, "REQ-001", "Description", "User", "Cost Center",
                RequisitionStatus.PENDING, null, null, null, null,
                LocalDateTime.now(), List.of(new RequisitionItemResponse(1L, "Product", "SKU-001", 10, "Notes"))
        );

        when(requisitionService.createRequisition(any(CreateRequisitionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/requisitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requisitionNumber").value("REQ-001"));
    }

    @Test
    void getAllRequisitions_ReturnsPage() throws Exception {
        RequisitionResponse req = new RequisitionResponse(
                1L, "REQ-001", "Description", "User", "Cost Center",
                RequisitionStatus.PENDING, null, null, null, null,
                LocalDateTime.now(), Collections.emptyList());

        Page<RequisitionResponse> page = new PageImpl<>(List.of(req));
        when(requisitionService.getAllRequisitions(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/requisitions").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].requisitionNumber").value("REQ-001"));
    }

    @Test
    void getAllRequisitions_WithStatusFilter() throws Exception {
        Page<RequisitionResponse> page = new PageImpl<>(Collections.emptyList());
        when(requisitionService.getRequisitionsByStatus(any(RequisitionStatus.class), any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/requisitions")
                        .param("status", "PENDING")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getRequisition_Success() throws Exception {
        RequisitionResponse response = new RequisitionResponse(
                1L, "REQ-001", "Description", "User", "Cost Center",
                RequisitionStatus.PENDING, null, null, null, null,
                LocalDateTime.now(), Collections.emptyList());

        when(requisitionService.getRequisitionById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/requisitions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requisitionNumber").value("REQ-001"));
    }

    @Test
    void submitForApproval_Success() throws Exception {
        RequisitionResponse response = new RequisitionResponse(
                1L, "REQ-001", "Description", "User", "Cost Center",
                RequisitionStatus.AWAITING_APPROVAL, null, null, null, null,
                LocalDateTime.now(), Collections.emptyList());

        when(requisitionService.submitForApproval(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/requisitions/1/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"));
    }

    @Test
    void approveRequisition_Success() throws Exception {
        RequisitionResponse response = new RequisitionResponse(
                1L, "REQ-001", "Description", "User", "Cost Center",
                RequisitionStatus.APPROVED, null, "Approved", "Approver", null,
                LocalDateTime.now(), Collections.emptyList());

        when(requisitionService.approveRequisition(eq(1L), any(UUID.class), eq("Approved"))).thenReturn(response);

        mockMvc.perform(patch("/api/requisitions/1/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"550e8400-e29b-41d4-a716-446655440000\",\"notes\":\"Approved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void rejectRequisition_Success() throws Exception {
        RequisitionResponse response = new RequisitionResponse(
                1L, "REQ-001", "Description", "User", "Cost Center",
                RequisitionStatus.REJECTED, "Rejected reason", null, null, null,
                LocalDateTime.now(), Collections.emptyList());

        when(requisitionService.rejectRequisition(eq(1L), any(UUID.class), eq("Rejected"))).thenReturn(response);

        mockMvc.perform(patch("/api/requisitions/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"550e8400-e29b-41d4-a716-446655440000\",\"reason\":\"Rejected\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void cancelRequisition_Success() throws Exception {
        RequisitionResponse response = new RequisitionResponse(
                1L, "REQ-001", "Description", "User", "Cost Center",
                RequisitionStatus.CANCELLED, null, null, null, null,
                LocalDateTime.now(), Collections.emptyList());

        when(requisitionService.cancelRequisition(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/requisitions/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void markAsConverted_Success() throws Exception {
        RequisitionResponse response = new RequisitionResponse(
                1L, "REQ-001", "Description", "User", "Cost Center",
                RequisitionStatus.CONVERTED, null, null, null, null,
                LocalDateTime.now(), Collections.emptyList());

        when(requisitionService.markAsConverted(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/requisitions/1/convert"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONVERTED"));
    }
}
