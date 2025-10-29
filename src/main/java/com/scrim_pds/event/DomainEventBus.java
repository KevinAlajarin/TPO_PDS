package com.scrim_pds.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bus de eventos simple para desacoplar componentes (Diagrama).
 * Implementación básica en memoria y asíncrona.
 */
@Component
public class DomainEventBus {

    private static final Logger logger = LoggerFactory.getLogger(DomainEventBus.class);

    // Mapa para almacenar suscriptores por tipo de evento
    // Usamos ConcurrentHashMap para seguridad en hilos
    private final Map<Class<? extends DomainEvent>, List<Subscriber<? extends DomainEvent>>> subscribers = new ConcurrentHashMap<>();

    // Executor para manejar eventos de forma asíncrona
    private final ExecutorService executor = Executors.newCachedThreadPool(); // O usa un TaskExecutor de Spring

    /**
     * Registra un suscriptor para un tipo específico de evento.
     * @param subscriber El suscriptor a registrar.
     */
    public <T extends DomainEvent> void subscribe(Subscriber<T> subscriber) {
        Class<T> eventType = subscriber.listensTo();
        // computeIfAbsent asegura que la lista exista y añade el suscriptor de forma atómica
        subscribers.computeIfAbsent(eventType, k -> Collections.synchronizedList(new ArrayList<>())).add(subscriber);
        logger.info("Suscriptor {} registrado para el evento {}", subscriber.getClass().getSimpleName(), eventType.getSimpleName());
    }

    /**
     * Elimina un suscriptor para un tipo específico de evento.
     * @param subscriber El suscriptor a eliminar.
     */
    public <T extends DomainEvent> void unsubscribe(Subscriber<T> subscriber) {
         Class<T> eventType = subscriber.listensTo();
        List<Subscriber<? extends DomainEvent>> subs = subscribers.get(eventType);
        if (subs != null) {
            boolean removed = subs.remove(subscriber);
            if(removed) {
                logger.info("Suscriptor {} eliminado para el evento {}", subscriber.getClass().getSimpleName(), eventType.getSimpleName());
            }
            // Opcional: limpiar el mapa si la lista queda vacía
            // if (subs.isEmpty()) {
            //     subscribers.remove(eventType);
            // }
        }
    }


    /**
     * Publica un evento, notificando a todos los suscriptores interesados de forma asíncrona.
     * @param event El evento a publicar.
     */
    @SuppressWarnings("unchecked") // Necesario por el casteo genérico seguro
    public void publish(DomainEvent event) {
        if (event == null) {
            logger.warn("Se intentó publicar un evento nulo.");
            return;
        }
        Class<? extends DomainEvent> eventType = event.getClass();
        logger.debug("Publicando evento: {}", eventType.getSimpleName());

        // Obtener la lista de suscriptores para este tipo de evento (y sus superclases/interfaces si quisiéramos)
        List<Subscriber<? extends DomainEvent>> subsForEvent = subscribers.get(eventType);

        if (subsForEvent != null && !subsForEvent.isEmpty()) {
            // Copiar la lista para evitar ConcurrentModificationException si alguien se desuscribe mientras iteramos
             List<Subscriber<? extends DomainEvent>> subscribersToNotify = new ArrayList<>(subsForEvent);

            // Notificar a cada suscriptor en un hilo separado
            for (Subscriber subscriber : subscribersToNotify) {
                executor.submit(() -> { // Envía la tarea al pool de hilos
                    try {
                        logger.debug("Notificando a suscriptor {} sobre evento {}", subscriber.getClass().getSimpleName(), eventType.getSimpleName());
                        subscriber.onEvent(event); // Llama al método onEvent
                    } catch (Exception e) {
                        // Capturar excepciones para que un suscriptor fallido no detenga a otros
                        logger.error("Error al notificar al suscriptor {} sobre el evento {}: {}",
                                     subscriber.getClass().getSimpleName(), eventType.getSimpleName(), e.getMessage(), e);
                    }
                });
            }
        } else {
            logger.debug("No hay suscriptores para el evento {}", eventType.getSimpleName());
        }
    }

    // Opcional: Método para cerrar el ExecutorService al apagar la app
    // @PreDestroy
    // public void shutdown() {
    //     logger.info("Apagando DomainEventBus ExecutorService...");
    //     executor.shutdown();
    // }
}
