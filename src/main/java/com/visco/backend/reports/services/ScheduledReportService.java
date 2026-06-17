package com.visco.backend.reports.services;

import com.visco.backend.reports.models.dtos.ReportDTO;
import com.visco.backend.reports.models.entities.ScheduledReport;
import com.visco.backend.reports.models.enums.ReportStatus;
import com.visco.backend.reports.repositories.ScheduledReportRepository;
import com.visco.backend.reports.utils.DateUtils;
import com.visco.backend.services.ResendEmailService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * Periodically executes scheduled reports and distributes them by email.
 */
public class ScheduledReportService {

  private final ScheduledReportRepository scheduledReportRepository;
  private final ReportService reportService;
  private final ResendEmailService resendEmailService;

  @Value("${app.reports.email-link-base-url:https://viscoorinocosia.vercel.app}")
  private String reportsPublicUrl;

  /**
   * Runs every hour, checks for due scheduled reports, generates them, and emails recipients.
   */
  @Scheduled(cron = "0 0 * * * *")
  @Transactional
  public void executeScheduledReports() {
    log.info("Checking for scheduled reports to execute...");
    List<ScheduledReport> due =
      scheduledReportRepository.findByEnabledAndNextExecutionAtLessThanEqual(
        true,
        LocalDateTime.now()
      );

    for (ScheduledReport sr : due) {
      try {
        log.info(
          "Executing scheduled report: {} (ID: {})",
          sr.getName(),
          sr.getId()
        );

        var reportDTO = reportService.executeScheduledReport(sr.getId());

        if (
          reportDTO.getStatus() == ReportStatus.COMPLETED &&
          sr.getRecipientEmails() != null &&
          !sr.getRecipientEmails().isBlank()
        ) {
          List<String> emails = Arrays.asList(
            sr.getRecipientEmails().split(",")
          );
          sendReportByEmail(reportDTO, emails);
        }

        LocalDateTime nextExec = DateUtils.calculateNextExecution(
          sr.getScheduleTime(),
          sr.getScheduleDayOfWeek(),
          sr.getScheduleDay()
        );
        sr.setNextExecutionAt(nextExec);
        sr.setLastExecutedAt(LocalDateTime.now());
        scheduledReportRepository.save(sr);

        log.info(
          "Scheduled report {} executed successfully, next execution: {}",
          sr.getName(),
          nextExec
        );
      } catch (Exception e) {
        log.error(
          "Error executing scheduled report {}: {}",
          sr.getName(),
          e.getMessage(),
          e
        );
      }
    }
  }

  private void sendReportByEmail(ReportDTO reportDTO, List<String> emails) {
    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    String subject = "Reporte: " + reportDTO.getName() + " - " + today;

    String inAppLink = reportsPublicUrl + "/reports/" + reportDTO.getId();
    String text = String.format(
      "Adjunto encontrara el reporte '%s' generado automaticamente.%n%n" +
      "Tipo: %s%nRegistros: %s%nFecha: %s%n%n" +
      "Descarguelo desde la aplicacion: %s",
      reportDTO.getName(),
      reportDTO.getType().getDisplayName(),
      reportDTO.getRecordCount() != null ? reportDTO.getRecordCount() : "N/A",
      today,
      inAppLink
    );

    for (String email : emails) {
      email = email.trim();
      if (email.isBlank()) continue;
      resendEmailService.sendHtmlEmail(email, subject, text);
      log.info("Report {} link sent to {}", reportDTO.getId(), email);
    }
  }
}
