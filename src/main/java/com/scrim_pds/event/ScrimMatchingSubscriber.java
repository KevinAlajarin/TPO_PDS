package com.scrim_pds.event;

import com.scrim_pds.model.PreferenciasUsuario;
import com.scrim_pds.model.Scrim;
import com.scrim_pds.model.User;
import com.scrim_pds.model.enums.CanalNotificacion;
import com.scrim_pds.notification.NotificationService;
import com.scrim_pds.persistence.JsonPersistenceManager; 
import jakarta.annotation.PostConstruct; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

// Suscriptor que escucha ScrimCreatedEvent y notifica a usuarios con preferencias coincidentes.

@Component
public class ScrimMatchingSubscriber implements Subscriber<ScrimCreatedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ScrimMatchingSubscriber.class);

    private final DomainEventBus eventBus;
    private final JsonPersistenceManager persistenceManager; 
    private final NotificationService notificationService; 

    public ScrimMatchingSubscriber(DomainEventBus eventBus,
                                   JsonPersistenceManager persistenceManager,
                                   NotificationService notificationService) {
        this.eventBus = eventBus;
        this.persistenceManager = persistenceManager;
        this.notificationService = notificationService;
    }

    @PostConstruct
    public void subscribeToEvents() {
        eventBus.subscribe(this);
    }

    @Override
    public Class<ScrimCreatedEvent> listensTo() {
        return ScrimCreatedEvent.class; 
    }

    @Override
    public void onEvent(ScrimCreatedEvent event) {
        Scrim newScrim = event.scrim();
        if (newScrim == null) {
            logger.warn("ScrimMatchingSubscriber recibió un ScrimCreatedEvent con Scrim nulo.");
            return;
        }
        logger.info("Procesando ScrimCreatedEvent para Scrim ID: {}", newScrim.getId());

        try {
            List<User> allUsers = persistenceManager.readCollection("users.json", User.class);
            int notifiedCount = 0;

            for (User user : allUsers) {
                // Ignorar al organizador del scrim
                if (user.getId().equals(newScrim.getOrganizadorId())) {
                    continue;
                }

                PreferenciasUsuario prefs = user.getPreferencias();
                if (prefs != null && prefs.isAlertasScrim() && matchesPreferences(newScrim, prefs)) {

                    // Verificar si el usuario quiere notificaciones por EMAIL
                    if (prefs.getCanalesNotificacion() != null &&
                        prefs.getCanalesNotificacion().contains(CanalNotificacion.EMAIL.name())) {

                        // Enviar notificacion (el NotificationService se encargara de llamar al EmailNotifier)
                        notificationService.sendNewScrimNotification(user, newScrim);
                        notifiedCount++;
                    }
                }
            }
            logger.info("Notificación de nuevo scrim enviada a {} usuarios.", notifiedCount);

        } catch (IOException e) {
            logger.error("Error al leer users.json para notificar sobre nuevo scrim {}: {}", newScrim.getId(), e.getMessage());
        } catch (Exception e) {
             logger.error("Error inesperado procesando ScrimCreatedEvent para {}: {}", newScrim.getId(), e.getMessage(), e);
        }
    }

    // Verifica si un Scrim coincide con las preferencias de búsqueda de un usuario.

    private boolean matchesPreferences(Scrim scrim, PreferenciasUsuario prefs) {
        boolean match = true; 

        // Comprobar juego (si el usuario tiene preferencia)
        if (prefs.getBusquedaJuegoPorDefecto() != null && !prefs.getBusquedaJuegoPorDefecto().isEmpty()) {
            if (!scrim.getJuego().equalsIgnoreCase(prefs.getBusquedaJuegoPorDefecto())) {
                match = false; // No coincide el juego
            }
        }

        // Comprobar region (si el usuario tiene preferencia y coincide)
        if (match && prefs.getBusquedaRegionPorDefecto() != null && !prefs.getBusquedaRegionPorDefecto().isEmpty()) {
            if (!scrim.getRegion().equalsIgnoreCase(prefs.getBusquedaRegionPorDefecto())) {
                match = false; // No coincide la region
            }
        }

        // Comprobar rangos
        boolean rangoMatch = true; // Asumir que coincide si no hay preferencias de rango
        if (prefs.getBusquedaRangoMinPorDefecto() != null && !prefs.getBusquedaRangoMinPorDefecto().isEmpty() &&
            prefs.getBusquedaRangoMaxPorDefecto() != null && !prefs.getBusquedaRangoMaxPorDefecto().isEmpty())
        {
             rangoMatch = scrim.getRangoMin().equalsIgnoreCase(prefs.getBusquedaRangoMinPorDefecto()) &&
                          scrim.getRangoMax().equalsIgnoreCase(prefs.getBusquedaRangoMaxPorDefecto());
        }
        if (!rangoMatch) {
            match = false;
        }

        // Aca podemos agregar mas filtros (modalidad, formato, etc)

        if (match) {
             logger.debug("Scrim {} coincide con preferencias de usuario {}", scrim.getId(), prefs); 
        }
        return match;
    }
}
