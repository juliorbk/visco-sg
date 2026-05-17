package com.visco.backend.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.visco.backend.models.dtos.CriticalInventoryItemDTO;
import com.visco.backend.models.dtos.KpiStatsDTO;
import com.visco.backend.models.dtos.RecentOrderDTO;
import com.visco.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class WeeklyReportService {

    private final StatsService statsService;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${report.recipients:}")
    private String reportRecipients;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Corre todos los lunes a las 8:00 AM
    @Scheduled(cron = "0 0 8 * * MON")
    @Transactional(readOnly = true)
    public void sendWeeklyReport() {
        log.info("Generando reporte semanal...");

        List<String> recipients = resolveRecipients();
        if (recipients.isEmpty()) {
            log.warn("No hay destinatarios configurados para el reporte semanal. "
                    + "Agrega report.recipients en el .env");
            return;
        }

        try {
            KpiStatsDTO kpis = statsService.getKpis();
            List<RecentOrderDTO> recentOrders = statsService.getRecentOrders(10);
            List<CriticalInventoryItemDTO> critical = statsService.getCriticalInventory();

            String html = buildHtml(kpis, recentOrders, critical);
            String subject = String.format("📊 Reporte Semanal Visco Orinoco — %s",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            for (String recipient : recipients) {
                sendEmail(recipient.trim(), subject, html);
            }

            log.info("Reporte semanal enviado a {} destinatario(s)", recipients.size());

        } catch (Exception e) {
            log.error("Error al generar/enviar reporte semanal: {}", e.getMessage(), e);
        }
    }

    // También disparo manual desde un endpoint si hace falta (útil para pruebas)
    @Transactional(readOnly = true)
    public void sendReportNow() {
        sendWeeklyReport();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<String> resolveRecipients() {
        // 1. Primero intenta la variable de entorno report.recipients
        if (reportRecipients != null && !reportRecipients.isBlank()) {
            return List.of(reportRecipients.split(","));
        }
        // 2. Fallback: todos los MANAGER y ADMIN activos de la DB
        return userRepository.findAll().stream().filter(u -> Boolean.TRUE.equals(u.getActive()))
                .filter(u -> u.getRole().name().equals("ADMIN")
                        || u.getRole().name().equals("MANAGER"))
                .map(u -> u.getEmail()).toList();
    }

    private void sendEmail(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
        } catch (MessagingException e) {
            log.error("❌ No se pudo enviar reporte a {}: {}", to, e.getMessage());
        }
    }

    // ── HTML del reporte ──────────────────────────────────────────────────────

    private String buildHtml(KpiStatsDTO kpis, List<RecentOrderDTO> orders,
            List<CriticalInventoryItemDTO> critical) {

        String generatedAt = LocalDateTime.now().format(DATE_FMT);
        String fulfillment = String.format("%.1f%%", kpis.getFulfillmentRate());
        String spend = formatMoney(kpis.getMonthlySpend());
        String units = kpis.getTotalInventoryUnits() != null
                ? kpis.getTotalInventoryUnits().toPlainString()
                : "0";

        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0;padding:0;background:#F5F5F7;font-family:'Segoe UI',Arial,sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background:#F5F5F7;padding:32px 0;">
                  <tr><td align="center">
                  <table width="620" cellpadding="0" cellspacing="0"
                         style="background:#ffffff;border-radius:16px;overflow:hidden;
                                box-shadow:0 8px 24px rgba(0,0,0,0.07);">

                    <!-- HEADER -->
                    <tr>
                      <td style="background:linear-gradient(135deg,#5C1212,#A0302A);
                                  padding:32px 40px 28px;">
                        <p style="margin:0 0 4px;font-size:11px;font-weight:600;
                                   color:rgba(255,255,255,0.55);letter-spacing:2px;
                                   text-transform:uppercase;">Informe automático</p>
                        <h1 style="margin:0;font-size:26px;font-weight:700;color:#fff;">
                          Reporte Semanal
                        </h1>
                        <p style="margin:6px 0 0;font-size:13px;color:rgba(255,255,255,0.6);">
                          Generado el %s
                        </p>
                      </td>
                    </tr>

                    <!-- KPIs -->
                    <tr>
                      <td style="padding:32px 40px 0;">
                        <p style="margin:0 0 14px;font-size:11px;font-weight:600;
                                   color:#9CA3AF;letter-spacing:1.5px;text-transform:uppercase;">
                          Resumen de la semana
                        </p>
                        <table width="100%%" cellpadding="0" cellspacing="0"
                               style="border:1px solid #F3F4F6;border-radius:12px;overflow:hidden;">
                          <tr>
                            <td style="padding:18px 20px;border-right:1px solid #F3F4F6;text-align:center;">
                              <div style="font-size:22px;font-weight:700;color:#111827;">%d</div>
                              <div style="font-size:11px;color:#9CA3AF;margin-top:4px;
                                          text-transform:uppercase;letter-spacing:0.8px;">
                                Órdenes totales</div>
                            </td>
                            <td style="padding:18px 20px;border-right:1px solid #F3F4F6;text-align:center;">
                              <div style="font-size:22px;font-weight:700;color:#111827;">%s</div>
                              <div style="font-size:11px;color:#9CA3AF;margin-top:4px;
                                          text-transform:uppercase;letter-spacing:0.8px;">
                                Unidades en stock</div>
                            </td>
                            <td style="padding:18px 20px;border-right:1px solid #F3F4F6;text-align:center;">
                              <div style="font-size:22px;font-weight:700;color:#111827;">%s</div>
                              <div style="font-size:11px;color:#9CA3AF;margin-top:4px;
                                          text-transform:uppercase;letter-spacing:0.8px;">
                                Gasto mensual</div>
                            </td>
                            <td style="padding:18px 20px;text-align:center;">
                              <div style="font-size:22px;font-weight:700;color:#7B1A1A;">%s</div>
                              <div style="font-size:11px;color:#9CA3AF;margin-top:4px;
                                          text-transform:uppercase;letter-spacing:0.8px;">
                                Cumplimiento</div>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>

                    <!-- INVENTARIO CRÍTICO -->
                    %s

                    <!-- ÓRDENES RECIENTES -->
                    %s

                    <!-- FOOTER -->
                    <tr>
                      <td style="padding:24px 40px;border-top:1px solid #F3F4F6;
                                  margin-top:32px;text-align:center;">
                        <p style="margin:0;font-size:12px;color:#9CA3AF;">
                          Este reporte se genera automáticamente cada lunes a las 8:00 AM.<br>
                          © %d Visco Orinoco — Sistema de Gestión Empresarial
                        </p>
                      </td>
                    </tr>

                  </table>
                  </td></tr>
                </table>
                </body>
                </html>
                """
                .formatted(generatedAt, kpis.getTotalOrders(), units, spend, fulfillment,
                        buildCriticalSection(critical), buildOrdersSection(orders),
                        LocalDateTime.now().getYear());
    }

    private String buildCriticalSection(List<CriticalInventoryItemDTO> items) {
        if (items.isEmpty()) {
            return """
                    <tr><td style="padding:28px 40px 0;">
                      <p style="margin:0 0 14px;font-size:11px;font-weight:600;color:#9CA3AF;
                                 letter-spacing:1.5px;text-transform:uppercase;">
                        Inventario crítico</p>
                      <p style="margin:0;padding:16px;background:#F0FDF4;border-radius:8px;
                                 font-size:13px;color:#15803D;">
                        ✅ Todos los productos están sobre su punto de reorden.
                      </p>
                    </td></tr>
                    """;
        }

        StringBuilder rows = new StringBuilder();
        for (CriticalInventoryItemDTO item : items) {
            String color = "CRITICAL".equals(item.getSeverity()) ? "#FEF2F2" : "#FFFBEB";
            String badge = "CRITICAL".equals(item.getSeverity()) ? "#DC2626" : "#D97706";
            String label = "CRITICAL".equals(item.getSeverity()) ? "CRÍTICO" : "ALERTA";
            rows.append(
                    """
                            <tr style="border-bottom:1px solid #F3F4F6;">
                              <td style="padding:10px 16px;font-size:13px;color:#111827;background:%s;">
                                <strong>%s</strong>
                                <span style="margin-left:8px;font-size:10px;font-weight:700;color:#fff;
                                              background:%s;padding:2px 6px;border-radius:4px;">%s</span>
                              </td>
                              <td style="padding:10px 16px;font-size:12px;color:#6B7280;background:%s;">%s</td>
                              <td style="padding:10px 16px;font-size:13px;color:#111827;
                                          font-weight:600;text-align:right;background:%s;">
                                %s / %s
                              </td>
                            </tr>
                            """
                            .formatted(color, item.getProductName(), badge, label, color,
                                    item.getSku(), color, item.getCurrentStock().toPlainString(),
                                    item.getReorderPoint().toPlainString()));
        }

        return """
                <tr><td style="padding:28px 40px 0;">
                  <p style="margin:0 0 14px;font-size:11px;font-weight:600;color:#9CA3AF;
                             letter-spacing:1.5px;text-transform:uppercase;">
                    Inventario crítico (%d productos)</p>
                  <table width="100%%" cellpadding="0" cellspacing="0"
                         style="border:1px solid #F3F4F6;border-radius:12px;overflow:hidden;
                                font-size:13px;">
                    <tr style="background:#F9FAFB;">
                      <th style="padding:10px 16px;text-align:left;font-size:11px;
                                  color:#6B7280;font-weight:600;">Producto</th>
                      <th style="padding:10px 16px;text-align:left;font-size:11px;
                                  color:#6B7280;font-weight:600;">SKU</th>
                      <th style="padding:10px 16px;text-align:right;font-size:11px;
                                  color:#6B7280;font-weight:600;">Stock / Reorden</th>
                    </tr>
                    %s
                  </table>
                </td></tr>
                """.formatted(items.size(), rows.toString());
    }

    private String buildOrdersSection(List<RecentOrderDTO> orders) {
        if (orders.isEmpty()) {
            return "";
        }

        StringBuilder rows = new StringBuilder();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (RecentOrderDTO o : orders) {
            String statusColor = switch (o.getStatus().name()) {
                case "DELIVERED", "COMPLETED" -> "#15803D";
                case "CANCELLED", "REJECTED" -> "#DC2626";
                case "PENDING" -> "#D97706";
                default -> "#2563EB";
            };
            rows.append("""
                    <tr style="border-bottom:1px solid #F3F4F6;">
                      <td style="padding:10px 16px;font-size:13px;color:#111827;font-weight:500;">
                        %s</td>
                      <td style="padding:10px 16px;font-size:12px;color:#6B7280;">%s</td>
                      <td style="padding:10px 16px;font-size:12px;color:#6B7280;">%s</td>
                      <td style="padding:10px 16px;text-align:right;">
                        <span style="font-size:11px;font-weight:600;color:%s;">%s</span>
                      </td>
                    </tr>
                    """.formatted(o.getOrderNumber(), o.getSupplierName(),
                    o.getCreatedAt() != null ? o.getCreatedAt().format(fmt) : "—", statusColor,
                    o.getStatus().name()));
        }

        return """
                <tr><td style="padding:28px 40px 0;">
                  <p style="margin:0 0 14px;font-size:11px;font-weight:600;color:#9CA3AF;
                             letter-spacing:1.5px;text-transform:uppercase;">
                    Órdenes recientes</p>
                  <table width="100%%" cellpadding="0" cellspacing="0"
                         style="border:1px solid #F3F4F6;border-radius:12px;overflow:hidden;">
                    <tr style="background:#F9FAFB;">
                      <th style="padding:10px 16px;text-align:left;font-size:11px;
                                  color:#6B7280;font-weight:600;">N° Orden</th>
                      <th style="padding:10px 16px;text-align:left;font-size:11px;
                                  color:#6B7280;font-weight:600;">Proveedor</th>
                      <th style="padding:10px 16px;text-align:left;font-size:11px;
                                  color:#6B7280;font-weight:600;">Fecha</th>
                      <th style="padding:10px 16px;text-align:right;font-size:11px;
                                  color:#6B7280;font-weight:600;">Estado</th>
                    </tr>
                    %s
                  </table>
                </td></tr>
                """.formatted(rows.toString());
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null)
            return "$0.00";
        return "$" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
                .replaceAll("(\\d)(?=(\\d{3})+\\.)", "$1,");
    }
}
