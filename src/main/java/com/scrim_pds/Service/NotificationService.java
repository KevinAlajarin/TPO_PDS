package com.scrim_pds.notification;

import com.scrim_pds.model.Scrim;
import com.scrim_pds.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.UUID; // <-- IMPORT AÑADIDO

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final Notifier emailNotifier;
    // Podríamos inyectar otros notifiers (Push, Discord) aquí si los tuviéramos
    // private final Notifier pushNotifier;

    // Formateador para fechas/horas en emails (puedes ajustar el estilo y Locale)
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM) // MEDIUM = "Oct 29, 2025, 1:15:30 PM" (ejemplo)
            .withLocale(Locale.getDefault()); // Usa el locale del servidor (puedes forzar "es-AR" etc.)


    // Inyectamos el EmailNotifier específico usando @Qualifier
    public NotificationService(@Qualifier("emailNotifier") Notifier emailNotifier) {
        this.emailNotifier = emailNotifier;
    }

    /**
     * Envía una notificación de bienvenida con link de verificación.
     * @param newUser El usuario recién registrado.
     * @param verificationLink El link de verificación a incluir.
     */
    public void sendWelcomeNotification(User newUser, String verificationLink) {
        String destinatario = newUser.getEmail();
        String asunto = "¡Bienvenido a eScrim! Verifica tu email";
        String cuerpo = "Hola " + newUser.getUsername() + ",\n\n" +
                        "Gracias por registrarte en eScrim.\n\n" +
                        "Para completar tu registro, por favor haz clic en el siguiente enlace para verificar tu dirección de email:\n" +
                        verificationLink + "\n\n" +
                        "(Si no te registraste, puedes ignorar este email).\n\n" +
                        "Saludos,\nEl equipo de eScrim";

        logger.info("Intentando enviar email de verificación/bienvenida a {}", destinatario);
        boolean enviado = emailNotifier.send(destinatario, asunto, cuerpo);
        logResult("Verificación/Bienvenida", enviado, null, destinatario); // Usar helper
    }


    /**
     * Envía una notificación por email sobre un nuevo Scrim que coincide con preferencias.
     * @param recipient El usuario a notificar.
     * @param scrim El nuevo scrim que coincide.
     */
    public void sendNewScrimNotification(User recipient, Scrim scrim) {
        String destinatario = recipient.getEmail();
        String asunto = "¡Nuevo Scrim disponible que podría interesarte!";
        String fechaFormateada = scrim.getFechaHora() != null ? scrim.getFechaHora().format(DATE_TIME_FORMATTER) : "N/A";

        String cuerpo = String.format(
            "Hola %s,\n\n" +
            "Se ha creado un nuevo scrim que coincide con tus preferencias:\n\n" +
            "Juego: %s\n" +
            "Región: %s\n" +
            "Formato: %s\n" +
            "Rango: %s - %s\n" +
            "Fecha: %s\n" + // Usar fecha formateada
            "Descripción: %s\n\n" +
            "¡Puedes buscarlo en la plataforma!\n\n" +
            "Saludos,\nEl equipo de eScrim",
            recipient.getUsername(),
            scrim.getJuego() != null ? scrim.getJuego() : "N/A",
            scrim.getRegion() != null ? scrim.getRegion() : "N/A",
            scrim.getFormato() != null ? scrim.getFormato() : "N/A",
            scrim.getRangoMin() != null ? scrim.getRangoMin() : "N/A",
            scrim.getRangoMax() != null ? scrim.getRangoMax() : "N/A",
            fechaFormateada, // <-- Usar fecha formateada
            scrim.getDescripcion() != null && !scrim.getDescripcion().isEmpty() ? scrim.getDescripcion() : "(Sin descripción)"
        );

        logger.info("Intentando enviar notificación de nuevo scrim a {}", destinatario);
        boolean enviado = emailNotifier.send(destinatario, asunto, cuerpo);
        logResult("Nuevo Scrim", enviado, scrim.getId(), destinatario); // Usar helper
    }

    /**
     * Envía una notificación por email indicando que el Lobby está listo.
     * @param recipient El usuario (organizador o postulante) a notificar.
     * @param scrim El scrim cuyo lobby se armó.
     */
    public void sendLobbyArmadoNotification(User recipient, Scrim scrim) {
        String destinatario = recipient.getEmail();
        String asunto = "¡Lobby Armado para tu Scrim de " + (scrim.getJuego() != null ? scrim.getJuego() : "Juego Desconocido") + "!";
        String fechaFormateada = scrim.getFechaHora() != null ? scrim.getFechaHora().format(DATE_TIME_FORMATTER) : "N/A";

        String cuerpo = String.format(
            "Hola %s,\n\n" +
            "¡El lobby para el scrim de %s (%s) está completo!\n\n" + // Incluir ID para referencia
            "Detalles del Scrim:\n" +
            " - Juego: %s\n" +
            " - Región: %s\n" +
            " - Fecha: %s\n\n" + // Usar fecha formateada
            "El siguiente paso es que todos los participantes confirmen su asistencia.\n" +
            "Ve a la plataforma para confirmar tu participación.\n" +
            "Recibirás otra notificación una vez que todos hayan confirmado.\n\n" +
            "Saludos,\nEl equipo de eScrim",
            recipient.getUsername(),
            scrim.getJuego() != null ? scrim.getJuego() : "N/A",
            scrim.getId(),
            scrim.getJuego() != null ? scrim.getJuego() : "N/A",
            scrim.getRegion() != null ? scrim.getRegion() : "N/A",
            fechaFormateada // <-- Usar fecha formateada
        );

        logger.info("Intentando enviar notificación de Lobby Armado a {}", destinatario);
        boolean enviado = emailNotifier.send(destinatario, asunto, cuerpo);
        logResult("Lobby Armado", enviado, scrim.getId(), destinatario); // Usar helper
    }

    /**
     * Envía una notificación por email indicando que el Scrim está confirmado y listo para iniciar.
     * @param recipient El usuario (organizador o participante) a notificar.
     * @param scrim El scrim que fue confirmado.
     */
    public void sendScrimConfirmadoNotification(User recipient, Scrim scrim) {
        String destinatario = recipient.getEmail();
        String asunto = "¡Scrim Confirmado! Prepárense para " + (scrim.getJuego() != null ? scrim.getJuego() : "la partida");
        String fechaFormateada = scrim.getFechaHora() != null ? scrim.getFechaHora().format(DATE_TIME_FORMATTER) : "Próximamente";

        String cuerpo = String.format(
            "Hola %s,\n\n" +
            "¡Todos los participantes han confirmado para el scrim de %s (%s)!\n\n" + // Incluir ID
            "El scrim está programado para comenzar el: %s\n\n" + // Usar fecha formateada
            "¡Prepárate para la partida!\n" +
            "\n" +
            "Saludos,\nEl equipo de eScrim",
            recipient.getUsername(),
            scrim.getJuego() != null ? scrim.getJuego() : "N/A",
            scrim.getId(), // Incluir ID
            fechaFormateada // Usar fecha formateada
        );

        logger.info("Intentando enviar notificación de Scrim Confirmado a {}", destinatario);
        boolean enviado = emailNotifier.send(destinatario, asunto, cuerpo);
        logResult("Scrim Confirmado", enviado, scrim.getId(), destinatario); // Usar helper
    }

    // --- NUEVOS MÉTODOS AÑADIDOS ---

    /**
     * Envía notificación de que el Scrim ha iniciado.
     */
    public void sendScrimIniciadoNotification(User recipient, Scrim scrim) {
        String destinatario = recipient.getEmail();
        String asunto = "¡Tu Scrim de " + (scrim.getJuego() != null ? scrim.getJuego() : "N/A") + " ha comenzado!";
        String fechaFormateada = scrim.getFechaHora() != null ? scrim.getFechaHora().format(DATE_TIME_FORMATTER) : "Ahora";

        String cuerpo = String.format(
            "Hola %s,\n\n" +
            "¡El scrim de %s (%s) programado para %s acaba de comenzar!\n\n" + // Incluir ID
            "¡Mucha suerte en la partida!\n\n" +
            "Saludos,\nEl equipo de eScrim",
            recipient.getUsername(),
            scrim.getJuego() != null ? scrim.getJuego() : "N/A",
            scrim.getId(), // Incluir ID
            fechaFormateada
        );

        logger.info("Intentando enviar notificación de Scrim Iniciado a {}", destinatario);
        boolean enviado = emailNotifier.send(destinatario, asunto, cuerpo);
        logResult("Scrim Iniciado", enviado, scrim.getId(), destinatario);
    }

    /**
     * Envía notificación de que el Scrim ha finalizado.
     */
    public void sendScrimFinalizadoNotification(User recipient, Scrim scrim) {
        String destinatario = recipient.getEmail();
        String asunto = "¡Tu Scrim de " + (scrim.getJuego() != null ? scrim.getJuego() : "N/A") + " ha finalizado!";
        boolean esOrganizador = recipient.getId().equals(scrim.getOrganizadorId());
        String mensajeExtra = esOrganizador ? "Ya puedes cargar las estadísticas en la plataforma." : "Pronto estarán disponibles las estadísticas.";

        String cuerpo = String.format(
            "Hola %s,\n\n" +
            "El scrim de %s (%s) ha finalizado.\n\n" + // Incluir ID
            "%s\n\n" + // Mensaje extra
            "¡Esperamos que hayas tenido una buena partida!\n\n" +
            "Saludos,\nEl equipo de eScrim",
            recipient.getUsername(),
            scrim.getJuego() != null ? scrim.getJuego() : "N/A",
            scrim.getId(), // Incluir ID
            mensajeExtra
        );

        logger.info("Intentando enviar notificación de Scrim Finalizado a {}", destinatario);
        boolean enviado = emailNotifier.send(destinatario, asunto, cuerpo);
        logResult("Scrim Finalizado", enviado, scrim.getId(), destinatario);
    }

    /**
     * Envía notificación de que el Scrim ha sido cancelado.
     */
    public void sendScrimCanceladoNotification(User recipient, Scrim scrim) {
        String destinatario = recipient.getEmail();
        String asunto = "Scrim Cancelado: " + (scrim.getJuego() != null ? scrim.getJuego() : "N/A");

        String cuerpo = String.format(
            "Hola %s,\n\n" +
            "Lamentamos informarte que el scrim de %s (%s) ha sido cancelado por el organizador.\n\n" + // Incluir ID
            "Ya no necesitas participar en esta partida.\n\n" +
            "Puedes buscar otros scrims disponibles en la plataforma.\n\n" +
            "Saludos,\nEl equipo de eScrim",
            recipient.getUsername(),
            scrim.getJuego() != null ? scrim.getJuego() : "N/A",
            scrim.getId() // Incluir ID
        );

        logger.info("Intentando enviar notificación de Scrim Cancelado a {}", destinatario);
        boolean enviado = emailNotifier.send(destinatario, asunto, cuerpo);
        logResult("Scrim Cancelado", enviado, scrim.getId(), destinatario);
    }

    // Método helper privado para loguear resultado del envío
    // Asegurarse que UUID esté importado
    private void logResult(String notificationType, boolean success, UUID scrimId, String recipientEmail) {
        String scrimIdStr = scrimId != null ? scrimId.toString() : "N/A"; // Manejar posible null para bienvenida
        if (success) {
            logger.info("Email de {} ({}) enviado exitosamente a {}", notificationType, scrimIdStr, recipientEmail);
        } else {
            logger.error("Falló el envío del email de {} ({}) a {}", notificationType, scrimIdStr, recipientEmail);
        }
    }
}

