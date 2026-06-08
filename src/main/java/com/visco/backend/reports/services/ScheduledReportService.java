package com.visco.backend.reports.services;

import com.visco.backend.reports.models.dtos.ReportDTO;
import com.visco.backend.reports.models.entities.ScheduledReport;
import com.visco.backend.reports.models.enums.ReportFormat;
import com.visco.backend.reports.models.enums.ReportStatus;
import com.visco.backend.reports.repositories.ScheduledReportRepository;
import com.visco.backend.reports.utils.DateUtils;
import com.visco.backend.services.ResendEmailService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    for (String email : emails) {
      email = email.trim();
      if (email.isBlank()) continue;

      String subject =
        "Reporte: " +
        reportDTO.getName() +
        " - " +
        java.time.LocalDate.now()
          .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

      String text = String.format(
        "Adjunto encontrara el reporte '%s' generado automaticamente.\n\n" +
        "Tipo: %s\nRegistros: %s\nFecha: %s",
        reportDTO.getName(),
        reportDTO.getType().getDisplayName(),
        reportDTO.getRecordCount() != null
          ? reportDTO.getRecordCount()
          : "N/A",
        java.time.LocalDate.now()
          .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
      );

      if (reportDTO.getFilePath() != null) {
        File file = Path.of(reportDTO.getFilePath()).toFile();
        if (file.exists()) {
          try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            String ext = reportDTO.getFormat() == ReportFormat.PDF
              ? ".pdf"
              : ".xlsx";
            String filename =
              reportDTO.getName().replaceAll("[^a-zA-Z0-9\\-_]", "_") + ext;
            resendEmailService.sendEmailWithAttachment(
              email,
              subject,
              text,
              filename,
              fileBytes,
              ext.equals(".pdf")
                ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );
            log.info("Report sent to {}", email);
          } catch (IOException e) {
            log.error(
              "Failed to read attachment file {}: {}",
              file.getPath(),
              e.getMessage()
            );
            resendEmailService.sendHtmlEmail(email, subject, text);
          }
        } else {
          resendEmailService.sendHtmlEmail(email, subject, text);
        }
      } else {
        resendEmailService.sendHtmlEmail(email, subject, text);
      }
    }
  }
}
