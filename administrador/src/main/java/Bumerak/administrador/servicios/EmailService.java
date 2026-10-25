package Bumerak.administrador.servicios;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.nombre:Bumerak}")
    private String nombreRemitente;

    /**
     * Envía las credenciales de acceso a un perfil independiente.
     * Se ejecuta en un hilo aparte para no bloquear la transacción principal.
     */
    @Async
    public void enviarCredencialesPerfil(String destinatario,
                                         String nombrePerfil,
                                         String emailAcceso,
                                         String passwordTemporal) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from, nombreRemitente);
            helper.setTo(destinatario);
            helper.setSubject("Tus credenciales de acceso - Bumerak");

            String html = construirHtmlCredenciales(nombrePerfil, emailAcceso, passwordTemporal);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("📧 Credenciales enviadas a {}", destinatario);
        } catch (Exception e) {
            log.error("❌ Error al enviar email a {}: {}", destinatario, e.getMessage(), e);
            // No relanzamos: si el correo falla, el perfil ya está creado y no queremos revertirlo.
        }
    }

    /**
     * Reenvía credenciales (por si el correo se perdió o el usuario no lo recibió).
     */
    @Async
    public void reenviarCredencialesPerfil(String destinatario,
                                           String nombrePerfil,
                                           String emailAcceso,
                                           String passwordTemporal) {
        enviarCredencialesPerfil(destinatario, nombrePerfil, emailAcceso, passwordTemporal);
    }

    /**
     * Construye el HTML del correo de credenciales.
     */
    private String construirHtmlCredenciales(String nombrePerfil,
                                             String emailAcceso,
                                             String passwordTemporal) {
        return """
            <div style="font-family: Arial, Helvetica, sans-serif; max-width: 600px; margin: 0 auto; color:#111827;">
                <div style="background:#2563eb; padding:24px; border-radius:8px 8px 0 0;">
                    <h1 style="color:#ffffff; margin:0; font-size:22px;">Bumerak</h1>
                </div>
                <div style="padding:24px; border:1px solid #e5e7eb; border-top:none; border-radius:0 0 8px 8px;">
                    <h2 style="color:#111827; margin-top:0;">¡Hola, %s!</h2>

                    <p>Se ha creado un perfil para ti en <strong>Bumerak</strong> con acceso independiente.</p>

                    <p>Estas son tus credenciales para iniciar sesión:</p>

                    <div style="background:#f3f4f6; padding:16px; border-radius:8px; margin:16px 0;">
                        <p style="margin:4px 0;">
                            <strong>Email:</strong> %s
                        </p>
                        <p style="margin:4px 0;">
                            <strong>Contraseña temporal:</strong>
                            <code style="background:#ffffff; padding:2px 6px; border-radius:4px; font-family: Consolas, monospace;">%s</code>
                        </p>
                    </div>

                    <p style="color:#b45309; background:#fef3c7; padding:12px; border-radius:6px;">
                        <strong>Importante:</strong> al iniciar sesión por primera vez, cambia tu contraseña
                        desde la sección de tu perfil.
                    </p>

                    <p>Si tienes cualquier problema para acceder, contacta con el administrador que creó tu perfil.</p>

                    <p style="color:#6b7280; font-size:12px; margin-top:24px;">
                        Si no esperabas este correo, puedes ignorarlo.
                    </p>
                </div>
            </div>
            """.formatted(nombrePerfil, emailAcceso, passwordTemporal);
    }
}