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

// Suscriptor que escucha LobbyArmadoEvent y notifica al organizador y participantes.

@Component
public class LobbyArmadoSubscriber implements Subscriber<LobbyArmadoEvent> {

    private static final Logger logger = LoggerFactory.getLogger(LobbyArmadoSubscriber.class);

    private final DomainEventBus eventBus;
    private final JsonPersistenceManager persistenceManager; 
    private final NotificationService notificationService;
    private final UserService userService; 

    public LobbyArmadoSubscriber(DomainEventBus eventBus,
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
    public Class<LobbyArmadoEvent> listensTo() {
        return LobbyArmadoEvent.class;
    }

    @Override
    public void onEvent(LobbyArmadoEvent event) {
        UUID scrimId = event.scrimId();
        logger.info("Procesando LobbyArmadoEvent para Scrim ID: {}", scrimId);

        try {
            // Releer el Scrim por si acaso, aunque el evento podría tener mas datos.
             List<Scrim> scrims = persistenceManager.readCollection("scrims.json", Scrim.class);
             Optional<Scrim> scrimOpt = scrims.stream().filter(s -> s.getId().equals(scrimId)).findFirst();
             if (scrimOpt.isEmpty()) {
                 logger.error("No se encontró el Scrim {} al procesar LobbyArmadoEvent.", scrimId);
                 return;
             }
             Scrim scrim = scrimOpt.get();


            // 1. Notificar al Organizador
            Optional<User> organizadorOpt = userService.findUserById(event.organizadorId());
            if (organizadorOpt.isPresent()) {
                User organizador = organizadorOpt.get();
                if (shouldNotify(organizador, CanalNotificacion.EMAIL)) { // Verificar preferencias
                    notificationService.sendLobbyArmadoNotification(organizador, scrim);
                } else {
                     logger.debug("Organizador {} no desea notificaciones EMAIL para Lobby Armado.", organizador.getId());
                }
            } else {
                logger.warn("No se encontró al organizador con ID {} para notificar sobre Lobby Armado del Scrim {}.", event.organizadorId(), scrimId);
            }

            // 2. Notificar a los Postulantes (PENDIENTE o ACEPTADA)
            List<Postulacion> postulaciones = persistenceManager.readCollection("postulaciones.json", Postulacion.class);
            List<Postulacion> postulantesActivos = postulaciones.stream()
                    .filter(p -> p.getScrimId().equals(scrimId) &&
                                 (p.getEstado() == PostulacionState.PENDIENTE || p.getEstado() == PostulacionState.ACEPTADA))
                    .collect(Collectors.toList());

            int notifiedPostulantes = 0;
            for (Postulacion p : postulantesActivos) {
                Optional<User> postulanteOpt = userService.findUserById(p.getUsuarioId());
                if (postulanteOpt.isPresent()) {
                    User postulante = postulanteOpt.get();
                    if (shouldNotify(postulante, CanalNotificacion.EMAIL)) {
                        notificationService.sendLobbyArmadoNotification(postulante, scrim);
                        notifiedPostulantes++;
                    } else {
                         logger.debug("Postulante {} no desea notificaciones EMAIL para Lobby Armado.", postulante.getId());
                    }
                } else {
                    logger.warn("No se encontró al usuario con ID {} (postulante) para notificar sobre Lobby Armado del Scrim {}.", p.getUsuarioId(), scrimId);
                }
            }
            logger.info("Notificación de Lobby Armado enviada a {} postulantes.", notifiedPostulantes);

        } catch (IOException e) {
            logger.error("Error al leer archivos para notificar sobre Lobby Armado del Scrim {}: {}", scrimId, e.getMessage());
        } catch (Exception e) {
             logger.error("Error inesperado procesando LobbyArmadoEvent para {}: {}", scrimId, e.getMessage(), e);
        }
    }

    // Verifica si un usuario desea recibir notificaciones por un canal específico.

    private boolean shouldNotify(User user, CanalNotificacion canal) {
        if (user.getPreferencias() == null) return false; // Sin preferencias, no notificar
        // Usamos la preferencia general 'alertasScrim' por ahora
        return user.getPreferencias().isAlertasScrim() &&
               user.getPreferencias().getCanalesNotificacion() != null &&
               user.getPreferencias().getCanalesNotificacion().contains(canal.name());
    }
}
