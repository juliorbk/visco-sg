package com.visco.backend.services;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class ResendEmailService {

  private final RestTemplate restTemplate;

  @Value("${resend.api.key}")
  private String apiKey;

  @Value("${app.mail.from}")
  private String fromAddress;

  @Value("${app.email.enabled:false}")
  private boolean emailEnabled;

  private static final String RESEND_API_URL = "https://api.resend.com/emails";
  private static final long MAX_ATTACHMENT_SIZE_BYTES = 25L * 1024 * 1024; // 25 MB

  public ResendEmailService() {
    this.restTemplate = new RestTemplate();
  }

  public void sendHtmlEmail(String to, String subject, String html) {
    if (!emailEnabled) {
      log.info("Email disabled — skipping send to {} | subject=\"{}\"", to, subject);
      return;
    }
    try {
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("from", fromAddress);
      body.put("to", List.of(to));
      body.put("subject", subject);
      body.put("html", html);

      sendRequest(body);
      log.info("Email sent to {} | subject=\"{}\"", to, subject);
    } catch (Exception e) {
      log.error("Failed to send email to {} | subject=\"{}\": {}", to, subject, e.getMessage(), e);
    }
  }

  public void sendEmailWithAttachment(
    String to,
    String subject,
    String text,
    String filename,
    byte[] content,
    String contentType
  ) {
    if (!emailEnabled) {
      log.info(
        "Email disabled — skipping send to {} | subject=\"{}\" | file={}",
        to,
        subject,
        filename
      );
      return;
    }
    if (content.length > MAX_ATTACHMENT_SIZE_BYTES) {
      log.warn("Attachment too large ({} MB) for email to {} — skipping. Use Cloudinary upload for files > 25 MB.",
        content.length / (1024 * 1024), to);
      return;
    }
    try {
      String base64Content = Base64.getEncoder().encodeToString(content);

      Map<String, Object> attachment = new LinkedHashMap<>();
      attachment.put("filename", filename);
      attachment.put("content", base64Content);
      attachment.put("content_type", contentType);

      Map<String, Object> body = new LinkedHashMap<>();
      body.put("from", fromAddress);
      body.put("to", List.of(to));
      body.put("subject", subject);
      if (text != null && !text.isEmpty()) {
        body.put("text", text);
      }
      body.put("attachments", List.of(attachment));

      sendRequest(body);
      log.info(
        "Email with attachment sent to {} | subject=\"{}\" | file={}",
        to,
        subject,
        filename
      );
    } catch (Exception e) {
      log.error(
        "Failed to send email with attachment to {}: {}",
        to,
        e.getMessage(),
        e
      );
    }
  }

  private void sendRequest(Map<String, Object> body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(apiKey);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    ResponseEntity<String> response = restTemplate.postForEntity(
      RESEND_API_URL,
      request,
      String.class
    );

    if (response.getStatusCode().is2xxSuccessful()) {
      log.debug("Resend API accepted: {}", response.getBody());
    } else {
      log.warn(
        "Resend API non-2xx: {} - {}",
        response.getStatusCode(),
        response.getBody()
      );
    }
  }
}
