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

@Component
public class ScrimIniciadoSubscriber implements Subscriber<ScrimIniciadoEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ScrimIniciadoSubscriber.class);

    private final DomainEventBus eventBus;
    private final JsonPersistenceManager persistenceManager;
    private final NotificationService notificationService;
    private final UserService userService;

    public ScrimIniciadoSubscriber(DomainEventBus eventBus, JsonPersistenceManager persistenceManager, NotificationService notificationService, UserService userService) {
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
    public Class<ScrimIniciadoEvent> listensTo() {
        return ScrimIniciadoEvent.class;
    }

    @Override
    public void onEvent(ScrimIniciadoEvent event) {
        UUID scrimId = event.scrimId();
        logger.info("Procesando ScrimIniciadoEvent para Scrim ID: {}", scrimId);

        try {
            // Releer Scrim para asegurar estado EN_JUEGO
            List<Scrim> scrims = persistenceManager.readCollection("scrims.json", Scrim.class);
            Optional<Scrim> scrimOpt = scrims.stream().filter(s -> s.getId().equals(scrimId)).findFirst();
            if (scrimOpt.isEmpty()) {
                 logger.error("No se encontró el Scrim {} al procesar ScrimIniciadoEvent.", scrimId);
                 return;
             }
            Scrim scrim = scrimOpt.get();

            // Notificar Organizador
            userService.findUserById(event.organizadorId()).ifPresent(organizador -> {
                if (shouldNotify(organizador, CanalNotificacion.EMAIL)) {
                    notificationService.sendScrimIniciadoNotification(organizador, scrim);
                }
            });

            // Notificar Participantes (Aceptados)
            List<Postulacion> postulaciones = persistenceManager.readCollection("postulaciones.json", Postulacion.class);
            postulaciones.stream()
                    .filter(p -> p.getScrimId().equals(scrimId) && p.getEstado() == PostulacionState.ACEPTADA)
                    .forEach(p -> userService.findUserById(p.getUsuarioId()).ifPresent(participante -> {
                        if (shouldNotify(participante, CanalNotificacion.EMAIL)) {
                            notificationService.sendScrimIniciadoNotification(participante, scrim);
                        }
                    }));

        } catch (IOException e) {
            logger.error("Error al leer archivos para notificar Scrim Iniciado {}: {}", scrimId, e.getMessage());
        } catch (Exception e) {
             logger.error("Error inesperado procesando ScrimIniciadoEvent para {}: {}", scrimId, e.getMessage(), e);
        }
    }

    private boolean shouldNotify(User user, CanalNotificacion canal) {
        if (user.getPreferencias() == null) return false;
        // Usar alertasScrim para esta notificación
        return user.getPreferencias().isAlertasScrim() &&
               user.getPreferencias().getCanalesNotificacion() != null &&
               user.getPreferencias().getCanalesNotificacion().contains(canal.name());
    }
}
