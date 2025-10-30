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
public class ScrimCanceladoSubscriber implements Subscriber<ScrimCanceladoEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ScrimCanceladoSubscriber.class);

    private final DomainEventBus eventBus;
    private final JsonPersistenceManager persistenceManager;
    private final NotificationService notificationService;
    private final UserService userService;

    public ScrimCanceladoSubscriber(DomainEventBus eventBus, JsonPersistenceManager persistenceManager, NotificationService notificationService, UserService userService) {
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
    public Class<ScrimCanceladoEvent> listensTo() {
        return ScrimCanceladoEvent.class;
    }

    @Override
    public void onEvent(ScrimCanceladoEvent event) {
        UUID scrimId = event.scrimId();
        logger.info("Procesando ScrimCanceladoEvent para Scrim ID: {}", scrimId);

        try {
             // Releer Scrim (necesario para detalles en email)
             List<Scrim> scrims = persistenceManager.readCollection("scrims.json", Scrim.class);
             Optional<Scrim> scrimOpt = scrims.stream().filter(s -> s.getId().equals(scrimId)).findFirst();
             if (scrimOpt.isEmpty()) { logger.error("Scrim {} no encontrado.", scrimId); return; }
             Scrim scrim = scrimOpt.get();

             // 1. Notificar Organizador (no se le notifica al organizador porque es el quien lo cancela,
             // si implementamos que un admin pueda cancelar un scrim entonces si deberiamos notificarle)
             userService.findUserById(event.organizadorId()).ifPresent(organizador -> {
                 if (shouldNotify(organizador, CanalNotificacion.EMAIL)) {
                     // notificationService.sendScrimCanceladoNotification(organizador, scrim); 
                     logger.debug("Omitiendo notificación de cancelación al organizador {}", organizador.getId());
                 }
             });

             // 2. Notificar a los Postulantes/Participantes (PENDIENTE o ACEPTADA)
             List<Postulacion> postulaciones = persistenceManager.readCollection("postulaciones.json", Postulacion.class);
             postulaciones.stream()
                     .filter(p -> p.getScrimId().equals(scrimId) &&
                                  (p.getEstado() == PostulacionState.PENDIENTE || p.getEstado() == PostulacionState.ACEPTADA))
                     .forEach(p -> userService.findUserById(p.getUsuarioId()).ifPresent(participante -> {
                         if (shouldNotify(participante, CanalNotificacion.EMAIL)) {
                             notificationService.sendScrimCanceladoNotification(participante, scrim);
                         }
                     }));

         } catch (IOException e) {
             logger.error("Error al leer archivos para notificar Scrim Cancelado {}: {}", scrimId, e.getMessage());
         } catch (Exception e) {
              logger.error("Error inesperado procesando ScrimCanceladoEvent para {}: {}", scrimId, e.getMessage(), e);
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
