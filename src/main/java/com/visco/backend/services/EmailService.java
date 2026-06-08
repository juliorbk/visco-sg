package com.visco.backend.services;

import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

  private final ResendEmailService resendEmailService;

  @Value("${app.mail.from}")
  private String fromAddress;

  @Value("${app.password-reset.base-url:https://viscoorinocosia.vercel.app}")
  private String passwordResetBaseUrl;

  @Value("${app.invite.base-url:https://viscoorinocosia.vercel.app}")
  private String inviteBaseUrl;

  public EmailService(ResendEmailService resendEmailService) {
    this.resendEmailService = resendEmailService;
  }

  // ── Public API ─────────────────────────────────────────────────────────

  /**
   * Sends a welcome email to a newly registered user.
   *
   * @param toEmail  the recipient email
   * @param userName the recipient's name
   */
  @Async
  public void sendWelcomeEmail(String toEmail, String userName) {
    String subject = "Tu acceso a Visco Orinoco está listo";
    String html = buildWelcomeHtml(userName);
    send(toEmail, subject, html);
  }

  /**
   * Sends a password reset email with a single-use token link.
   *
   * @param toEmail  the recipient email
   * @param userName the recipient's name
   * @param token    the reset token
   */
  @Async
  public void sendPasswordResetEmail(String toEmail, String userName, String token) {
    String subject = "Restablece tu contraseña — Visco Orinoco";
    String resetUrl = passwordResetBaseUrl +
      "/reset-password?token=" + java.net.URLEncoder.encode(token, StandardCharsets.UTF_8);
    String html = buildPasswordResetHtml(userName, resetUrl);
    send(toEmail, subject, html);
  }

  /**
   * Sends an invitation email with a registration link.
   *
   * @param toEmail      the recipient email
   * @param invitedName  the invited person's name
   * @param token        the invite token
   * @param intendedRole the role assigned to the invite
   */
  @Async
  public void sendInviteEmail(
    String toEmail,
    String invitedName,
    String token,
    String intendedRole
  ) {
    String subject = "Has sido invitado a Visco Orinoco";
    String registerUrl = inviteBaseUrl +
      "/register?token=" + java.net.URLEncoder.encode(token, StandardCharsets.UTF_8);
    String html = buildInviteHtml(
      invitedName != null ? invitedName : toEmail,
      registerUrl,
      intendedRole
    );
    send(toEmail, subject, html);
  }

  /**
   * Sends a confirmation email after a successful password change.
   *
   * @param toEmail  the recipient email
   * @param userName the recipient's name
   */
  @Async
  public void sendPasswordChangedEmail(String toEmail, String userName) {
    String subject = "Tu contraseña fue actualizada";
    String html = buildPasswordChangedHtml(userName);
    send(toEmail, subject, html);
  }

  // ── Transport ──────────────────────────────────────────────────────────

  private void send(String to, String subject, String htmlBody) {
    resendEmailService.sendHtmlEmail(to, subject, htmlBody);
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

  private String buildInviteHtml(String invitedName, String registerUrl, String intendedRole) {
    String roleLabel = intendedRole != null
      ? "Rol asignado: " + intendedRole
      : "";
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
                                      Estás invitado a<br/>Visco Orinoco.
                                  </p>
                                  <p style="margin:0;font-size:13px;color:rgba(255,255,255,0.55);line-height:1.6;">
                                      Únete a la plataforma de gestión empresarial.
                                  </p>
                              </td>
                          </tr>
                          <tr>
                              <td style="padding:44px 44px 36px 44px;">
                                  <p style="margin:0 0 8px 0;font-size:13px;font-weight:600;color:#9CA3AF;text-transform:uppercase;letter-spacing:1.5px;">
                                      Tienes una invitación activa
                                  </p>
                                  <h1 style="margin:0 0 16px 0;font-family:Georgia,'Times New Roman',serif;font-size:26px;font-weight:600;color:#111827;line-height:1.3;">
                                      Hola, <span style="color:#7B1A1A;">%s</span> 👋
                                  </h1>
                                  <p style="margin:0 0 16px 0;font-size:15px;color:#6B7280;line-height:1.7;">
                                      Has sido invitado a formar parte de <strong style="color:#374151;">Visco Orinoco</strong>.
                                      Completa tu registro para acceder a la plataforma y comenzar a gestionar
                                      órdenes de compra, inventario y proveedores.
                                  </p>
                                  %s
                                  <table cellpadding="0" cellspacing="0" style="margin:8px 0 28px 0;">
                                      <tr>
                                          <td style="border-radius:10px;background:#7B1A1A;">
                                              <a href="%s" target="_blank" style="display:inline-block;padding:14px 32px;font-family:'Segoe UI',Arial,sans-serif;font-size:14px;font-weight:600;color:#ffffff;text-decoration:none;letter-spacing:0.3px;">
                                                  Completar registro →
                                              </a>
                                          </td>
                                      </tr>
                                  </table>
                                  <p style="margin:0;font-size:13px;color:#9CA3AF;line-height:1.6;">
                                      Este enlace expira en <strong>72 horas</strong> y solo puede usarse una vez.<br/>
                                      Si no esperabas esta invitación, puedes ignorar este correo de forma segura.
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
                                  <p style="margin:0 0 6px 0;font-size:12px;font-weight:600;color:#374151;">
                                      Visco Orinoco — Enterprise Tier
                                  </p>
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
      invitedName,
      roleLabel.isEmpty() ? "" : "<p style=\"margin:0 0 16px 0;font-size:14px;color:#374151;\"><strong>" + roleLabel + "</strong></p>",
      registerUrl,
      java.time.Year.now().getValue()
    );
  }
}
