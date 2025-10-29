package com.scrim_pds.event;

/**
 * Interfaz para los oyentes de eventos de dominio (Diagrama).
 */
public interface Subscriber<T extends DomainEvent> { // Genérico para type safety

    /**
     * Método llamado cuando se publica un evento del tipo suscrito.
     * @param event El evento publicado.
     */
    void onEvent(T event);

    /**
     * Devuelve la clase del evento al que este suscriptor está interesado.
     * @return La clase del evento.
     */
    Class<T> listensTo();
}
