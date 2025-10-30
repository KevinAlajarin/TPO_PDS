package com.scrim_pds.event;

// Interfaz para los oyentes de eventos de dominio.

public interface Subscriber<T extends DomainEvent> { 

    /**
     * Metodo llamado cuando se publica un evento del tipo suscrito.
     * @param event El evento publicado.
     */
    void onEvent(T event);

    /**
     * Devuelve la clase del evento al que este suscriptor esta interesado.
     * @return La clase del evento.
     */
    Class<T> listensTo();
}
