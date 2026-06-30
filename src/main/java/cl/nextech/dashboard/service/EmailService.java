package cl.nextech.dashboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.frontend-url}")
    private String frontendUrl;

    public void sendPasswordReset(String toEmail, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;

        String html = """
            <div style="font-family:Arial,sans-serif;max-width:520px;margin:0 auto;padding:32px;background:#f8fafc;border-radius:12px;">
              <div style="text-align:center;margin-bottom:24px;">
                <h2 style="color:#1e3a5f;margin:0;">Módulo de Facturación RS Tech</h2>
              </div>
              <div style="background:#fff;border-radius:8px;padding:28px;border:1px solid #e2e8f0;">
                <h3 style="margin-top:0;color:#1e293b;">Recuperación de contraseña</h3>
                <p style="color:#64748b;line-height:1.6;">
                  Recibimos una solicitud para restablecer la contraseña de tu cuenta.<br>
                  Haz clic en el botón para crear una nueva contraseña:
                </p>
                <div style="text-align:center;margin:28px 0;">
                  <a href="%s"
                     style="background:#1a56db;color:#fff;padding:12px 32px;border-radius:8px;text-decoration:none;font-weight:600;font-size:15px;">
                    Restablecer contraseña
                  </a>
                </div>
                <p style="color:#94a3b8;font-size:12px;margin-bottom:0;">
                  Este enlace expira en <strong>2 horas</strong>.<br>
                  Si no solicitaste este cambio, ignora este correo.
                </p>
              </div>
            </div>
            """.formatted(link);

        try {
            var message = mailSender.createMimeMessage();
            var helper  = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject("Restablecer contraseña — RS Tech");
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email de reset enviado a {}", toEmail);
        } catch (Exception e) {
            log.error("Error enviando email de reset a {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("No se pudo enviar el correo de recuperación");
        }
    }
}
