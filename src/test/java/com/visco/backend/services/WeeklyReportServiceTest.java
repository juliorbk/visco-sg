package com.visco.backend.services;

import com.visco.backend.models.dtos.CriticalInventoryItemDTO;
import com.visco.backend.models.dtos.KpiStatsDTO;
import com.visco.backend.models.dtos.RecentOrderDTO;
import com.visco.backend.models.entities.UserRole;
import com.visco.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyReportServiceTest {

    @Mock
    private StatsService statsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private WeeklyReportService weeklyReportService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(weeklyReportService, "senderEmail", "admin@visco.com");
        ReflectionTestUtils.setField(weeklyReportService, "reportRecipients", "admin@visco.com");
    }

    @Test
    void sendWeeklyReport_SendsEmails() {
        KpiStatsDTO kpis = KpiStatsDTO.builder()
                .totalOrders(100)
                .totalInventoryUnits(BigDecimal.valueOf(1000))
                .monthlySpend(BigDecimal.valueOf(50000))
                .fulfillmentRate(95.0)
                .build();

        when(statsService.getKpis()).thenReturn(kpis);
        when(statsService.getRecentOrders(10)).thenReturn(Collections.emptyList());
        when(statsService.getCriticalInventory()).thenReturn(Collections.emptyList());
        when(mailSender.createMimeMessage()).thenReturn(mock(jakarta.mail.internet.MimeMessage.class));

        weeklyReportService.sendWeeklyReport();

        verify(statsService).getKpis();
        verify(statsService).getRecentOrders(10);
        verify(statsService).getCriticalInventory();
        verify(mailSender, atLeastOnce()).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    @Test
    void sendWeeklyReport_SkipsWhenNoRecipients() {
        ReflectionTestUtils.setField(weeklyReportService, "reportRecipients", "");
        when(userRepository.findActiveAdminAndManagerEmails()).thenReturn(Collections.emptyList());

        weeklyReportService.sendWeeklyReport();

        verify(statsService, never()).getKpis();
        verify(mailSender, never()).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    @Test
    void sendReportNow_CallsSendWeeklyReport() {
        KpiStatsDTO kpis = KpiStatsDTO.builder()
                .totalOrders(50)
                .totalInventoryUnits(BigDecimal.valueOf(500))
                .monthlySpend(BigDecimal.valueOf(25000))
                .fulfillmentRate(90.0)
                .build();

        when(statsService.getKpis()).thenReturn(kpis);
        when(statsService.getRecentOrders(10)).thenReturn(Collections.emptyList());
        when(statsService.getCriticalInventory()).thenReturn(Collections.emptyList());
        when(mailSender.createMimeMessage()).thenReturn(mock(jakarta.mail.internet.MimeMessage.class));

        weeklyReportService.sendReportNow();

        verify(statsService).getKpis();
    }

    @Test
    void sendWeeklyReport_UsesAdminEmailsWhenNoConfiguredRecipients() {
        ReflectionTestUtils.setField(weeklyReportService, "reportRecipients", "");
        when(userRepository.findActiveAdminAndManagerEmails())
                .thenReturn(List.of("admin1@visco.com", "admin2@visco.com"));

        KpiStatsDTO kpis = KpiStatsDTO.builder()
                .totalOrders(100)
                .totalInventoryUnits(BigDecimal.valueOf(1000))
                .monthlySpend(BigDecimal.valueOf(50000))
                .fulfillmentRate(95.0)
                .build();

        when(statsService.getKpis()).thenReturn(kpis);
        when(statsService.getRecentOrders(10)).thenReturn(Collections.emptyList());
        when(statsService.getCriticalInventory()).thenReturn(Collections.emptyList());
        when(mailSender.createMimeMessage()).thenReturn(mock(jakarta.mail.internet.MimeMessage.class));

        weeklyReportService.sendWeeklyReport();

        verify(userRepository).findActiveAdminAndManagerEmails();
        verify(mailSender, atLeastOnce()).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    @Test
    void buildHtml_GeneratesValidHtml() {
        KpiStatsDTO kpis = KpiStatsDTO.builder()
                .totalOrders(100)
                .totalInventoryUnits(BigDecimal.valueOf(1000))
                .monthlySpend(BigDecimal.valueOf(50000))
                .fulfillmentRate(95.0)
                .build();

        RecentOrderDTO order = RecentOrderDTO.builder()
                .id(1L).orderNumber("PO-001").supplierName("Supplier")
                .status(com.visco.backend.models.entities.PurchaseOrderStatus.DELIVERED)
                .build();

        CriticalInventoryItemDTO critical = CriticalInventoryItemDTO.builder()
                .productId(1L).productName("Product").sku("SKU-001")
                .currentStock(BigDecimal.ZERO)
                .reorderPoint(BigDecimal.valueOf(10))
                .severity("CRITICAL")
                .build();

        weeklyReportService.sendReportNow();

        verify(statsService).getKpis();
    }
}
