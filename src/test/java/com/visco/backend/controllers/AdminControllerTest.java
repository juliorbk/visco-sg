package com.visco.backend.controllers;

import com.visco.backend.services.WeeklyReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeeklyReportService weeklyReportService;

    @MockitoBean
    private org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration webSecurityConfiguration;

    @Test
    void sendReportNow_Success() throws Exception {
        doNothing().when(weeklyReportService).sendReportNow();

        mockMvc.perform(post("/api/admin/reports/send"))
                .andExpect(status().isOk())
                .andExpect(content().string("Reporte enviado. Revisa los logs para confirmar la entrega."));

        verify(weeklyReportService).sendReportNow();
    }
}
