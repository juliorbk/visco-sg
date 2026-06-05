package com.visco.backend.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Async email service backed by Spring's {@link JavaMailSender} (SMTP).
 *
 * <p>All send methods are {@code @Async} and run on the dedicated
 * {@code emailExecutor} thread pool so HTTP requests never block on
 * outbound SMTP.
 *
 * <p>The sender is read from {@code app.mail.from} (e.g. {@code "Visco
 * Orinoco <noreply@viscorinoco.com>"}). The transport is configured via
 * standard {@code spring.mail.*} properties (host, port, username,
 * password, ssl / starttls). For Render we default to port 465 with SSL
 * because port 25 is blocked and port 587 is often restricted.
 */
@Service
@Slf4j
public class EmailService {

  private final JavaMailSender mailSender;

  @Value("${app.mail.from}")
  private String fromAddress;

  @Value("${app.password-reset.base-url:https://viscoorinocosia.vercel.app}")
  private String passwordResetBaseUrl;

  public EmailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  // ── Public API ─────────────────────────────────────────────────────────

  @Async
  public void sendWelcomeEmail(String toEmail, String userName) {
    String subject = "Tu acceso a Visco Orinoco está listo";
    String html = buildWelcomeHtml(userName);
    send(toEmail, subject, html);
  }

  @Async
  public void sendPasswordResetEmail(String toEmail, String userName, String token) {
    String subject = "Restablece tu contraseña — Visco Orinoco";
    String resetUrl = passwordResetBaseUrl +
      "/reset-password?token=" + java.net.URLEncoder.encode(token, StandardCharsets.UTF_8);
    String html = buildPasswordResetHtml(userName, resetUrl);
    send(toEmail, subject, html);
  }

  @Async
  public void sendPasswordChangedEmail(String toEmail, String userName) {
    String subject = "Tu contraseña fue actualizada";
    String html = buildPasswordChangedHtml(userName);
    send(toEmail, subject, html);
  }

  // ── Transport ──────────────────────────────────────────────────────────

  private void send(String to, String subject, String htmlBody) {
    try {
      MimeMessage msg = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(
        msg,
        false,
        StandardCharsets.UTF_8.name()
      );
      helper.setFrom(fromAddress);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlBody, true);
      mailSender.send(msg);
      log.info("📧 Email sent to {} | subject=\"{}\"", to, subject);
    } catch (MessagingException e) {
      log.error("❌ Failed to send email to {} | subject=\"{}\": {}", to, subject, e.getMessage(), e);
    } catch (Exception e) {
      log.error("❌ Unexpected error sending email to {}: {}", to, e.getMessage(), e);
    }
  }

  // ── HTML templates ─────────────────────────────────────────────────────

  private String buildWelcomeHtml(String userName) {
    return String.format(
      """
      <!DOCTYPE html>
      <html lang="es">
      <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
      </head>
      <body style="margin:0;padding:0;background-color:#F5F5F7;font-family:'Segoe UI',Arial,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#F5F5F7;padding:40px 0;">
              <tr>
                  <td align="center">
                      <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,0.08);">
                          <tr>
                              <td style="background:linear-gradient(135deg,#5C1212 0%%,#7B1A1A 50%%,#A0302A 100%%);padding:36px 44px;">
                                  <p style="margin:0 0 4px 0;font-family:Georgia,'Times New Roman',serif;font-style:italic;font-size:28px;font-weight:400;color:#ffffff;line-height:1.25;">
                                      Bienvenido al sistema de<br/>gestión empresarial.
                                  </p>
                                  <p style="margin:0;font-size:13px;color:rgba(255,255,255,0.55);line-height:1.6;">
                                      Controla inventario, proveedores y órdenes de compra desde una sola plataforma.
                                  </p>
                              </td>
                          </tr>
                          <tr>
                              <td style="padding:44px 44px 36px 44px;">
                                  <p style="margin:0 0 8px 0;font-size:13px;font-weight:600;color:#9CA3AF;text-transform:uppercase;letter-spacing:1.5px;">
                                      Tu acceso está listo
                                  </p>
                                  <h1 style="margin:0 0 16px 0;font-family:Georgia,'Times New Roman',serif;font-size:26px;font-weight:600;color:#111827;line-height:1.3;">
                                      Hola, <span style="color:#7B1A1A;">%s</span> 👋
                                  </h1>
                                  <p style="margin:0 0 28px 0;font-size:15px;color:#6B7280;line-height:1.7;">
                                      Tu cuenta en <strong style="color:#374151;">Visco Orinoco</strong> ha sido creada exitosamente.
                                      Ya puedes acceder al sistema para gestionar órdenes de compra, controlar el inventario
                                      y supervisar el rendimiento de tus proveedores.
                                  </p>
                                  <table cellpadding="0" cellspacing="0" style="margin-bottom:36px;">
                                      <tr>
                                          <td style="border-radius:10px;background:#7B1A1A;">
                                              <a href="https://viscoorinocosia.vercel.app/" target="_blank" style="display:inline-block;padding:14px 32px;font-family:'Segoe UI',Arial,sans-serif;font-size:14px;font-weight:600;color:#ffffff;text-decoration:none;letter-spacing:0.3px;">
                                                  Iniciar sesión →
                                              </a>
                                          </td>
                                      </tr>
                                  </table>
                              </td>
                          </tr>
                          <tr>
                              <td style="padding:0 44px;">
                                  <div style="border-top:1px solid #F3F4F6;"></div>
                              </td>
                          </tr>
                          <tr>
                              <td style="padding:24px 44px;text-align:center;">
                                  <p style="margin:0 0 6px 0;font-size:12px;font-weight:600;color:#374151;">
                                      Visco Orinoco — Enterprise Tier
                                  </p>
                                  <p style="margin:0;font-size:11px;color:#9CA3AF;">
                                      Si no solicitaste esta cuenta, ignora este correo o contacta a soporte.<br/>
                                      © %d Visco Orinoco. Todos los derechos reservados.
                                  </p>
                              </td>
                          </tr>
                      </table>
                  </td>
              </tr>
          </table>
      </body>
      </html>
      """,
      userName,
      java.time.Year.now().getValue()
    );
  }

  private String buildPasswordResetHtml(String userName, String resetUrl) {
    return String.format(
      """
      <!DOCTYPE html>
      <html lang="es">
      <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
      </head>
      <body style="margin:0;padding:0;background-color:#F5F5F7;font-family:'Segoe UI',Arial,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#F5F5F7;padding:40px 0;">
              <tr>
                  <td align="center">
                      <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,0.08);">
                          <tr>
                              <td style="background:linear-gradient(135deg,#5C1212 0%%,#7B1A1A 100%%);padding:36px 44px;">
                                  <p style="margin:0;font-family:Georgia,'Times New Roman',serif;font-style:italic;font-size:24px;color:#ffffff;line-height:1.25;">
                                      Restablece tu contraseña
                                  </p>
                              </td>
                          </tr>
                          <tr>
                              <td style="padding:44px 44px 36px 44px;">
                                  <h1 style="margin:0 0 16px 0;font-family:Georgia,'Times New Roman',serif;font-size:22px;font-weight:600;color:#111827;line-height:1.3;">
                                      Hola, <span style="color:#7B1A1A;">%s</span>
                                  </h1>
                                  <p style="margin:0 0 24px 0;font-size:15px;color:#6B7280;line-height:1.7;">
                                      Recibimos una solicitud para restablecer la contraseña de tu cuenta en
                                      <strong style="color:#374151;">Visco Orinoco</strong>.
                                      Haz clic en el siguiente botón para crear una nueva contraseña.
                                  </p>
                                  <table cellpadding="0" cellspacing="0" style="margin-bottom:24px;">
                                      <tr>
                                          <td style="border-radius:10px;background:#7B1A1A;">
                                              <a href="%s" target="_blank" style="display:inline-block;padding:14px 32px;font-family:'Segoe UI',Arial,sans-serif;font-size:14px;font-weight:600;color:#ffffff;text-decoration:none;letter-spacing:0.3px;">
                                                  Restablecer contraseña →
                                              </a>
                                          </td>
                                      </tr>
                                  </table>
                                  <p style="margin:0 0 8px 0;font-size:13px;color:#9CA3AF;line-height:1.6;">
                                      Este enlace expira en <strong>2 horas</strong> y solo puede usarse una vez.
                                  </p>
                                  <p style="margin:0;font-size:13px;color:#9CA3AF;line-height:1.6;">
                                      Si no solicitaste este cambio, puedes ignorar este correo de forma segura.
                                  </p>
                              </td>
                          </tr>
                          <tr>
                              <td style="padding:0 44px;">
                                  <div style="border-top:1px solid #F3F4F6;"></div>
                              </td>
                          </tr>
                          <tr>
                              <td style="padding:24px 44px;text-align:center;">
                                  <p style="margin:0;font-size:11px;color:#9CA3AF;">
                                      © %d Visco Orinoco. Todos los derechos reservados.
                                  </p>
                              </td>
                          </tr>
                      </table>
                  </td>
              </tr>
          </table>
      </body>
      </html>
      """,
      userName,
      resetUrl,
      java.time.Year.now().getValue()
    );
  }

  private String buildPasswordChangedHtml(String userName) {
    return String.format(
      """
      <!DOCTYPE html>
      <html lang="es">
      <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
      </head>
      <body style="margin:0;padding:0;background-color:#F5F5F7;font-family:'Segoe UI',Arial,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#F5F5F7;padding:40px 0;">
              <tr>
                  <td align="center">
                      <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,0.08);">
                          <tr>
                              <td style="background:linear-gradient(135deg,#15803D 0%%,#16A34A 100%%);padding:36px 44px;">
                                  <p style="margin:0;font-family:Georgia,'Times New Roman',serif;font-style:italic;font-size:24px;color:#ffffff;line-height:1.25;">
                                      Contraseña actualizada
                                  </p>
                              </td>
                          </tr>
                          <tr>
                              <td style="padding:44px 44px 36px 44px;">
                                  <h1 style="margin:0 0 16px 0;font-family:Georgia,'Times New Roman',serif;font-size:22px;font-weight:600;color:#111827;line-height:1.3;">
                                      Hola, <span style="color:#7B1A1A;">%s</span>
                                  </h1>
                                  <p style="margin:0 0 8px 0;font-size:15px;color:#6B7280;line-height:1.7;">
                                      Te confirmamos que la contraseña de tu cuenta en
                                      <strong style="color:#374151;">Visco Orinoco</strong> fue actualizada exitosamente.
                                  </p>
                                  <p style="margin:0;font-size:13px;color:#9CA3AF;line-height:1.6;">
                                      Si no realizaste este cambio, contacta a soporte de inmediato.
                                  </p>
                              </td>
                          </tr>
                          <tr>
                              <td style="padding:0 44px;">
                                  <div style="border-top:1px solid #F3F4F6;"></div>
                              </td>
                          </tr>
                          <tr>
                              <td style="padding:24px 44px;text-align:center;">
                                  <p style="margin:0;font-size:11px;color:#9CA3AF;">
                                      © %d Visco Orinoco. Todos los derechos reservados.
                                  </p>
                              </td>
                          </tr>
                      </table>
                  </td>
              </tr>
          </table>
      </body>
      </html>
      """,
      userName,
      java.time.Year.now().getValue()
    );
  }
}
