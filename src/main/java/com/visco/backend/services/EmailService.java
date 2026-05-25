package com.visco.backend.services;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

  private final Resend resend;

  // Asumo que aquí guardas el remitente, por ejemplo: "Visco Orinoco <onboarding@resend.dev>"
  @Value("${spring.mail.username}")
  private String senderEmail;

  public EmailService(@Value("${resend.api.key}") String apiKey) {
    this.resend = new Resend(apiKey);
  }

  public void sendWelcomeEmail(String toEmail, String userName) {
    String htmlBody = String.format(
      """
      <!DOCTYPE html>
      <html lang="es">
      <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <style>
              @import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@300;400;500;600&display=swap');
          </style>
      </head>
      <body style="margin:0;padding:0;background-color:#F5F5F7;font-family:'DM Sans',Arial,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#F5F5F7;padding:40px 0;">
              <tr>
                  <td align="center">
                      <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,0.08);">
                          <tr>
                              <td style="background:linear-gradient(135deg,#5C1212 0%%,#7B1A1A 50%%,#A0302A 100%%);padding:0;">
                                  <table width="100%%" cellpadding="0" cellspacing="0" style="background-image:radial-gradient(circle,rgba(255,255,255,0.05) 1px,transparent 1px);background-size:24px 24px;">
                                      <tr>
                                          <td style="padding:36px 44px 32px 44px;">
                                              <table width="100%%" cellpadding="0" cellspacing="0">
                                                  <tr>
                                                      <td style="vertical-align:middle;">
                                                          <svg width="160" height="58" viewBox="0 0 160 58" fill="none" xmlns="http://www.w3.org/2000/svg">
                                                              <text x="0" y="26" font-family="Georgia,'Times New Roman',serif" font-weight="700" font-size="26" fill="#ffffff" letter-spacing="1">V</text>
                                                              <rect x="22" y="2" width="2.5" height="24" fill="rgba(255,255,255,0.4)"/>
                                                              <text x="28" y="26" font-family="Georgia,'Times New Roman',serif" font-weight="700" font-size="26" fill="#ffffff" letter-spacing="1">SCO</text>
                                                              <text x="0" y="54" font-family="Georgia,'Times New Roman',serif" font-weight="700" font-size="26" fill="#ffffff" letter-spacing="1">OR</text>
                                                              <rect x="42" y="30" width="2.5" height="24" fill="rgba(255,255,255,0.4)"/>
                                                              <text x="48" y="54" font-family="Georgia,'Times New Roman',serif" font-weight="700" font-size="26" fill="#ffffff" letter-spacing="1">NOCO</text>
                                                          </svg>
                                                      </td>
                                                      <td style="text-align:right;color:rgba(255,255,255,0.5);font-size:10px;font-weight:600;letter-spacing:2px;text-transform:uppercase;vertical-align:middle;">
                                                          NEXUS
                                                      </td>
                                                  </tr>
                                              </table>
                                              <p style="margin:28px 0 4px 0;font-family:Georgia,'Times New Roman',serif;font-style:italic;font-size:28px;font-weight:400;color:#ffffff;line-height:1.25;">
                                                  Bienvenido al sistema de<br/>gestión empresarial.
                                              </p>
                                              <p style="margin:0;font-size:13px;color:rgba(255,255,255,0.55);line-height:1.6;">
                                                  Controla inventario, proveedores y órdenes de compra desde una sola plataforma.
                                              </p>
                                          </td>
                                      </tr>
                                  </table>
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
                                              <a href="https://viscoorinocosia.vercel.app/" target="_blank" style="display:inline-block;padding:14px 32px;font-family:'DM Sans',Arial,sans-serif;font-size:14px;font-weight:600;color:#ffffff;text-decoration:none;letter-spacing:0.3px;">
                                                  Iniciar sesión →
                                              </a>
                                          </td>
                                      </tr>
                                  </table>

                                  <table width="100%%" cellpadding="0" cellspacing="0" style="border:1px solid #F3F4F6;border-radius:12px;overflow:hidden;">
                                      <tr>
                                          <td style="padding:16px 20px;border-right:1px solid #F3F4F6;text-align:center;">
                                              <div style="font-size:18px;font-weight:700;color:#111827;">1,284</div>
                                              <div style="font-size:11px;color:#9CA3AF;margin-top:3px;text-transform:uppercase;letter-spacing:0.8px;">Pedidos totales</div>
                                          </td>
                                          <td style="padding:16px 20px;border-right:1px solid #F3F4F6;text-align:center;">
                                              <div style="font-size:18px;font-weight:700;color:#111827;">45,910</div>
                                              <div style="font-size:11px;color:#9CA3AF;margin-top:3px;text-transform:uppercase;letter-spacing:0.8px;">Unidades en stock</div>
                                          </td>
                                          <td style="padding:16px 20px;text-align:center;">
                                              <div style="font-size:18px;font-weight:700;color:#7B1A1A;">98.2%%</div>
                                              <div style="font-size:11px;color:#9CA3AF;margin-top:3px;text-transform:uppercase;letter-spacing:0.8px;">Tasa de cumplimiento</div>
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

    // Construimos las opciones del correo con Resend
    CreateEmailOptions params = CreateEmailOptions.builder()
      .from("onboarding@resend.dev")
      .to(toEmail)
      .subject("Tu acceso a Visco Orinoco está listo") // Agregué un Asunto que faltaba
      .html(htmlBody)
      .build();

    try {
      CreateEmailResponse data = resend.emails().send(params);
      log.info(
        "📧 Correo de bienvenida enviado exitosamente a: {}. ID: {}",
        toEmail,
        data.getId()
      );
    } catch (ResendException e) {
      log.error(
        "❌ Error al enviar el correo de bienvenida a {}: {}",
        toEmail,
        e.getMessage(),
        e
      );
    }
  }
}
