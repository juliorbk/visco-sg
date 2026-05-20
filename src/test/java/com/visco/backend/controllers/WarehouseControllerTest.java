package com.visco.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visco.backend.models.dtos.*;
import com.visco.backend.models.entities.MovementType;
import com.visco.backend.services.WarehouseService;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WarehouseController.class)
class WarehouseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WarehouseService warehouseService;

    @MockitoBean
    private org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration webSecurityConfiguration;

    @Test
    void getAllWarehouses_ReturnsPage() throws Exception {
        WarehouseDTO warehouse = WarehouseDTO.builder().id(1L).name("Test Warehouse").build();
        Page<WarehouseDTO> page = new PageImpl<>(List.of(warehouse));
        when(warehouseService.getAllWarehouses(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/warehouse").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Test Warehouse"));
    }

    @Test
    void getWarehouse_Success() throws Exception {
        WarehouseDTO warehouse = WarehouseDTO.builder().id(1L).name("Test Warehouse").build();
        when(warehouseService.getWarehouse(1L)).thenReturn(warehouse);

        mockMvc.perform(get("/api/warehouse/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Warehouse"));
    }

    @Test
    void createWarehouse_Success() throws Exception {
        CreateWarehouseRequest request = new CreateWarehouseRequest("Test WH", "Address", "Desc", "SAP001", 1L);
        WarehouseDTO response = WarehouseDTO.builder().id(1L).name("Test WH").build();
        when(warehouseService.createWarehouse(any(CreateWarehouseRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/warehouse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test WH"));
    }

    @Test
    void getStockBreakdownByProduct_Success() throws Exception {
        ProductStockBreakdown breakdown = ProductStockBreakdown.builder()
                .productId(1L).totalStock(BigDecimal.valueOf(100)).totalPendingStock(BigDecimal.ZERO)
                .warehouses(Collections.emptyList()).build();
        when(warehouseService.getStockBreakdownByProduct(1L)).thenReturn(breakdown);

        mockMvc.perform(get("/api/warehouse/products/1/stock-breakdown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1));
    }

    @Test
    void getGlobalStockSummary_Success() throws Exception {
        when(warehouseService.getGlobalStockSummary()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/warehouse/stock-summary"))
                .andExpect(status().isOk());
    }

    @Test
    void transferStock_Success() throws Exception {
        TransferStockRequest request = new TransferStockRequest(
                1L, 2L, 1L, BigDecimal.valueOf(10), "Transfer", BigDecimal.valueOf(50), 1L);
        doNothing().when(warehouseService).transferStock(any(TransferStockRequest.class));

        mockMvc.perform(post("/api/warehouse/stock/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void adjustStock_Success() throws Exception {
        AdjustStockRequest request = new AdjustStockRequest(
                1L, 1L, BigDecimal.valueOf(150), "Adjust", BigDecimal.valueOf(50), 1L);
        doNothing().when(warehouseService).adjustStock(any(AdjustStockRequest.class));

        mockMvc.perform(post("/api/warehouse/stock/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void receiveGoods_Success() throws Exception {
        ReceiveGoodsRequest request = new ReceiveGoodsRequest(
                1L, 1L, "Notes", List.of(new ReceiveGoodsRequest.ReceiveItem(1L, BigDecimal.valueOf(10))));
        GoodReceiptResponse response = new GoodReceiptResponse(
                1L, "VIS-1-123-abc", 1L, "PO-001", null, LocalDateTime.now(), "Notes", "User", Collections.emptyList());
        when(warehouseService.receiveGoods(eq(1L), any(ReceiveGoodsRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/warehouse/orders/1/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void getAllReceipts_ReturnsPage() throws Exception {
        Page<GoodReceiptResponse> page = new PageImpl<>(Collections.emptyList());
        when(warehouseService.getAllOrders(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/warehouse/receipts").param("page", "0").param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getMovements_ReturnsPage() throws Exception {
        Page<InventoryMovementResponse> page = new PageImpl<>(Collections.emptyList());
        when(warehouseService.getMovements(any(), any(), any(), any(), any(), any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/warehouse/movement")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk());
    }
}
