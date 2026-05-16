package com.visco.backend.controllers;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.visco.backend.models.dtos.CreateWarehouseRequest;
import com.visco.backend.models.dtos.GoodReceiptItemResponse;
import com.visco.backend.models.dtos.GoodReceiptResponse;
import com.visco.backend.models.dtos.ProductStockBreakdown;
import com.visco.backend.models.dtos.ReceiveGoodsRequest;
import com.visco.backend.models.dtos.WarehouseResponse;
import com.visco.backend.models.dtos.WarehouseStockSummary;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.repositories.UserRepository;
import com.visco.backend.services.JwtService;
import com.visco.backend.services.WarehouseService;

import jakarta.persistence.EntityNotFoundException;

@WebMvcTest(WarehouseController.class)
@AutoConfigureMockMvc(addFilters = false)
class WarehouseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WarehouseService warehouseService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

	@Test
	@WithMockUser(authorities = "MANAGER")
	void createWarehouse_shouldReturn201() throws Exception {
		when(warehouseService.createWarehouse(any(CreateWarehouseRequest.class)))
				.thenReturn(WarehouseResponse.builder().id(1L).name("New Warehouse").sapCenterCode("WH02").build());
		mockMvc.perform(post("/api/warehouse")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"New Warehouse\",\"physicalAddress\":\"Addr 123\",\"description\":\"Desc\",\"responsibleUserId\":\"00000000-0000-0000-0000-000000000001\",\"sapCenterCode\":\"WH02\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("New Warehouse"));
	}

	@Test
	@WithMockUser(authorities = "WAREHOUSEMAN")
	void getAllWarehouses_shouldReturn200() throws Exception {
        when(warehouseService.getAllWarehouses()).thenReturn(List.of(
                WarehouseResponse.builder().id(1L).name("Main").sapCenterCode("WH01").build()));
        mockMvc.perform(get("/api/warehouse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Main"));
    }

    @Test
    @WithMockUser(authorities = "WAREHOUSEMAN")
    void getStockBreakdownByProduct_shouldReturn200() throws Exception {
        when(warehouseService.getStockBreakdownByProduct(1L))
                .thenReturn(ProductStockBreakdown.builder()
                        .productId(1L).totalStock(BigDecimal.valueOf(100))
                        .totalPendingStock(BigDecimal.valueOf(50)).warehouses(List.of()).build());
        mockMvc.perform(get("/api/warehouse/products/1/stock-breakdown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStock").value(100));
    }

    @Test
    @WithMockUser(authorities = "WAREHOUSEMAN")
    void getGlobalStockSummary_shouldReturn200() throws Exception {
        when(warehouseService.getGlobalStockSummary()).thenReturn(List.of(
                WarehouseStockSummary.builder().warehouseId(1L).warehouseName("Main")
                        .totalStock(BigDecimal.valueOf(200)).totalPendingStock(BigDecimal.valueOf(50)).build()));
        mockMvc.perform(get("/api/warehouse/stock-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].warehouseName").value("Main"));
    }

    @Test
    @WithMockUser(authorities = "WAREHOUSEMAN")
    void receiveGoods_shouldReturn201() throws Exception {
        when(warehouseService.receiveGoods(eq(1L), any(ReceiveGoodsRequest.class)))
                .thenReturn(new GoodReceiptResponse(1L, "VIS-1-12345", 1L, "PO-001",
                        PurchaseOrderStatus.PARTIALLY_DELIVERED, LocalDateTime.now(), "",
                        List.of(new GoodReceiptItemResponse(1L, "Product", "SKU-001",
                                BigDecimal.TEN, BigDecimal.valueOf(5), BigDecimal.valueOf(-5)))));

        mockMvc.perform(post("/api/warehouse/orders/1/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1,\"receivedQuantity\":5}],\"notes\":\"Partial\",\"destinationLocationId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptNumber").value("VIS-1-12345"));
    }

    @Test
    @WithMockUser(authorities = "WAREHOUSEMAN")
    void getAllReceipts_shouldReturn200() throws Exception {
        when(warehouseService.getAllOrders(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/api/warehouse/receipts")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "WAREHOUSEMAN")
    void getReceiptsByOrderId_shouldReturn200() throws Exception {
        when(warehouseService.getReceiptsByOrderId(1L)).thenReturn(List.of());
        mockMvc.perform(get("/api/warehouse/orders/1/receipts")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "WAREHOUSEMAN")
    void getReceipt_shouldReturn200() throws Exception {
        when(warehouseService.getReceiptById(1L)).thenReturn(new GoodReceiptResponse(
                1L, "VIS-1-12345", 1L, "PO-001", PurchaseOrderStatus.DELIVERED,
                LocalDateTime.now(), "All good", List.of()));
        mockMvc.perform(get("/api/warehouse/receipts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptNumber").value("VIS-1-12345"));
    }

    @Test
    @WithMockUser(authorities = "WAREHOUSEMAN")
    void getReceipt_shouldReturn404() throws Exception {
        when(warehouseService.getReceiptById(99L)).thenThrow(new EntityNotFoundException("Not found"));
        mockMvc.perform(get("/api/warehouse/receipts/99")).andExpect(status().isNotFound());
    }

}
