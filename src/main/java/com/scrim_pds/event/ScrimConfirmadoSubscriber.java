package com.scrim_pds.event;

import com.scrim_pds.model.Postulacion;
import com.scrim_pds.model.Scrim;
import com.scrim_pds.model.User;
import com.scrim_pds.model.enums.CanalNotificacion;
import com.scrim_pds.model.enums.PostulacionState;
import com.scrim_pds.notification.NotificationService;
import com.scrim_pds.persistence.JsonPersistenceManager;
import com.scrim_pds.service.UserService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

// Suscriptor que escucha ScrimConfirmadoEvent y notifica al organizador y participantes.

@Component
public class ScrimConfirmadoSubscriber implements Subscriber<ScrimConfirmadoEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ScrimConfirmadoSubscriber.class);

    private final DomainEventBus eventBus;
    private final JsonPersistenceManager persistenceManager; 
    private final NotificationService notificationService;
    private final UserService userService; 
    public ScrimConfirmadoSubscriber(DomainEventBus eventBus,
                                     JsonPersistenceManager persistenceManager,
                                     NotificationService notificationService,
                                     UserService userService) {
        this.eventBus = eventBus;
        this.persistenceManager = persistenceManager;
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @PostConstruct
    public void subscribeToEvents() {
        eventBus.subscribe(this);
    }

    @Override
    public Class<ScrimConfirmadoEvent> listensTo() {
        return ScrimConfirmadoEvent.class;
    }

    @Override
    public void onEvent(ScrimConfirmadoEvent event) {
        UUID scrimId = event.scrimId();
        logger.info("Procesando ScrimConfirmadoEvent para Scrim ID: {}", scrimId);

        try {
            // Releer el Scrim por si acaso.
             List<Scrim> scrims = persistenceManager.readCollection("scrims.json", Scrim.class);
             Optional<Scrim> scrimOpt = scrims.stream().filter(s -> s.getId().equals(scrimId)).findFirst();
             if (scrimOpt.isEmpty()) {
                 logger.error("No se encontró el Scrim {} al procesar ScrimConfirmadoEvent.", scrimId);
                 return;
             }
             Scrim scrim = scrimOpt.get(); // Usar el scrim leido que tiene el estado CONFIRMADO.

            // 1. Notificar al Organizador
            Optional<User> organizadorOpt = userService.findUserById(event.organizadorId());
            if (organizadorOpt.isPresent()) {
                User organizador = organizadorOpt.get();
                if (shouldNotify(organizador, CanalNotificacion.EMAIL)) {
                    notificationService.sendScrimConfirmadoNotification(organizador, scrim);
                } else {
                     logger.debug("Organizador {} no desea notificaciones EMAIL para Scrim Confirmado.", organizador.getId());
                }
            } else {
                logger.warn("No se encontró al organizador con ID {} para notificar Scrim Confirmado {}.", event.organizadorId(), scrimId);
            }

            // 2. Notificar a los Postulantes ACEPTADOS (Confirmados)
            List<Postulacion> postulaciones = persistenceManager.readCollection("postulaciones.json", Postulacion.class);
            List<Postulacion> confirmados = postulaciones.stream()
                    .filter(p -> p.getScrimId().equals(scrimId) && p.getEstado() == PostulacionState.ACEPTADA)
                    .collect(Collectors.toList());

            int notifiedPostulantes = 0;
            for (Postulacion p : confirmados) {
                Optional<User> postulanteOpt = userService.findUserById(p.getUsuarioId());
                if (postulanteOpt.isPresent()) {
                    User postulante = postulanteOpt.get();
                    if (shouldNotify(postulante, CanalNotificacion.EMAIL)) {
                        notificationService.sendScrimConfirmadoNotification(postulante, scrim);
                        notifiedPostulantes++;
                    } else {
                         logger.debug("Participante {} no desea notificaciones EMAIL para Scrim Confirmado.", postulante.getId());
                    }
                } else {
                    logger.warn("No se encontró al usuario con ID {} (participante) para notificar Scrim Confirmado {}.", p.getUsuarioId(), scrimId);
                }
            }
            logger.info("Notificación de Scrim Confirmado enviada a {} participantes.", notifiedPostulantes);

        } catch (IOException e) {
            logger.error("Error al leer archivos para notificar Scrim Confirmado {}: {}", scrimId, e.getMessage());
        } catch (Exception e) {
             logger.error("Error inesperado procesando ScrimConfirmadoEvent para {}: {}", scrimId, e.getMessage(), e);
        }
    }

    // Verifica si un usuario desea recibir notificaciones por un canal especifico.

    private boolean shouldNotify(User user, CanalNotificacion canal) {
        if (user.getPreferencias() == null) return false;
        // Podemos usar 'alertasPostulacion' o 'alertasScrim' dependiendo de la granularidad deseada
        return user.getPreferencias().isAlertasScrim() &&
               user.getPreferencias().getCanalesNotificacion() != null &&
               user.getPreferencias().getCanalesNotificacion().contains(canal.name());
    }
}
