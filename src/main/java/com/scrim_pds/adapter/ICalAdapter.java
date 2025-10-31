package com.scrim_pds.adapter;

import com.scrim_pds.model.Scrim;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId; // <-- AÑADIR IMPORT
import java.util.Date; // <-- AÑADIR IMPORT

/**
 * Adapter para convertir un objeto Scrim de nuestro dominio
 * a un String en formato iCalendar (.ics) usando la librería ical4j.
 */
@Component
public class ICalAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ICalAdapter.class);

    /**
     * Genera el contenido de un archivo .ics para un Scrim específico.
     * @param scrim El Scrim a convertir.
     * @return Un String que representa el calendario en formato iCal.
     */
    public String generarEventoCalendario(Scrim scrim) {
        try {
            // 1. Validar datos de entrada
            if (scrim.getFechaHora() == null) {
                logger.warn("No se puede generar iCal para Scrim {} sin fechaHora.", scrim.getId());
                return null;
            }

            // 2. Definir fechas
            LocalDateTime startTimeLDT = scrim.getFechaHora();
            LocalDateTime endTimeLDT = startTimeLDT.plusMinutes(scrim.getDuracion() != null ? scrim.getDuracion() : 60);

            // --- CORRECCIÓN: Convertir LocalDateTime a java.util.Date ---
            ZoneId defaultZoneId = ZoneId.systemDefault();
            Date startDate = Date.from(startTimeLDT.atZone(defaultZoneId).toInstant());
            Date endDate = Date.from(endTimeLDT.atZone(defaultZoneId).toInstant());
            // --- FIN CORRECCIÓN ---


            // 3. Crear el evento (VEvent)
            String summary = String.format("Scrim de %s (%s)",
                                           scrim.getJuego() != null ? scrim.getJuego() : "Juego",
                                           scrim.getFormato() != null ? scrim.getFormato().name() : "N/A");

            // --- CORRECCIÓN: Usar los constructores con el tipo Date de ical4j ---
            VEvent event = new VEvent(new net.fortuna.ical4j.model.Date(startDate),
                                      new net.fortuna.ical4j.model.Date(endDate),
                                      summary);


            // 4. Añadir propiedades al evento
            event.getProperties().add(new Uid(scrim.getId().toString()));
            if (scrim.getDescripcion() != null && !scrim.getDescripcion().isEmpty()) {
                event.getProperties().add(new Description(scrim.getDescripcion()));
            }
            if (scrim.getRegion() != null) {
                event.getProperties().add(new Location(scrim.getRegion()));
            }

            // 5. Crear el Calendario
            Calendar icsCalendar = new Calendar();
            icsCalendar.getProperties().add(new ProdId("-//eScrim//Scrim Calendar v1.0//EN"));
            icsCalendar.getProperties().add(Version.VERSION_2_0);
            icsCalendar.getProperties().add(CalScale.GREGORIAN);

            // 6. Añadir el evento al calendario
            icsCalendar.getComponents().add(event);

            logger.info("Generado calendario iCal para Scrim ID: {}", scrim.getId());
            return icsCalendar.toString();

        } catch (Exception e) {
            logger.error("Error al generar archivo iCal para Scrim ID {}: {}", scrim.getId(), e.getMessage(), e);
            return null; // O lanzar una excepción personalizada
        }
    }
}

