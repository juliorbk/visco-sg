package com.visco.backend.reports.services;

import com.visco.backend.reports.models.dtos.ReportDTO;
import com.visco.backend.reports.models.entities.ScheduledReport;
import com.visco.backend.reports.models.enums.ReportFormat;
import com.visco.backend.reports.models.enums.ReportStatus;
import com.visco.backend.reports.repositories.ScheduledReportRepository;
import com.visco.backend.reports.utils.DateUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledReportService {

  private final ScheduledReportRepository scheduledReportRepository;
  private final ReportService reportService;
  private final JavaMailSender mailSender;

  @Value("${spring.mail.username}")
  private String senderEmail;

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
      try {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(senderEmail);
        helper.setTo(email);
        helper.setSubject(
          "Reporte: " +
            reportDTO.getName() +
            " - " +
            java.time.LocalDate.now().format(
              java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
            )
        );
        helper.setText(
          String.format(
            "Adjunto encontrará el reporte '%s' generado automáticamente.<br><br>" +
              "Tipo: %s<br>Registros: %s<br>Fecha: %s",
            reportDTO.getName(),
            reportDTO.getType().getDisplayName(),
            reportDTO.getRecordCount() != null
              ? reportDTO.getRecordCount()
              : "N/A",
            java.time.LocalDate.now().format(
              java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
            )
          ),
          true
        );

        if (reportDTO.getFilePath() != null) {
          File file = Path.of(reportDTO.getFilePath()).toFile();
          if (file.exists()) {
            String ext =
              reportDTO.getFormat() == ReportFormat.PDF ? ".pdf" : ".xlsx";
            helper.addAttachment(
              reportDTO.getName().replaceAll("[^a-zA-Z0-9\\-_]", "_") + ext,
              new FileSystemResource(file)
            );
          }
        }

        mailSender.send(msg);
        log.info("Report sent to {}", email);
      } catch (MessagingException e) {
        log.error("Failed to send report to {}: {}", email, e.getMessage());
      }
    }
  }
}
