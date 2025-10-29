package com.scrim_pds.service;

import com.scrim_pds.dto.EstadisticaRequest;
import com.scrim_pds.dto.PostulacionRequest;
import com.scrim_pds.dto.ScrimCreateRequest;
import com.scrim_pds.event.*; // <-- Importar TODOS los eventos
import com.scrim_pds.exception.InvalidScrimStateException;
import com.scrim_pds.exception.ScrimNotFoundException;
import com.scrim_pds.exception.UnauthorizedException;
import com.scrim_pds.model.Estadistica;
import com.scrim_pds.model.Postulacion;
import com.scrim_pds.model.Scrim;
import com.scrim_pds.model.User;
import com.scrim_pds.model.enums.PostulacionState;
import com.scrim_pds.model.enums.ScrimStateEnum;
import com.scrim_pds.persistence.JsonPersistenceManager;
import com.scrim_pds.service.UserService;
// Importar estrategias si se usaran realmente
// import com.scrim_pds.matchmaking.*;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ScrimService {

    private static final Logger logger = LoggerFactory.getLogger(ScrimService.class);
    private final JsonPersistenceManager persistenceManager;
    private final DomainEventBus eventBus;
    private final UserService userService;
    private final String SCRIMS_FILE = "scrims.json";
    private final String POSTULACIONES_FILE = "postulaciones.json";
    private final String ESTADISTICAS_FILE = "estadisticas.json";

    public ScrimService(JsonPersistenceManager persistenceManager,
                        DomainEventBus eventBus,
                        UserService userService) {
        this.persistenceManager = persistenceManager;
        this.eventBus = eventBus;
        this.userService = userService;
    }

    /**
     * Crea un nuevo Scrim y PUBLICA el evento ScrimCreatedEvent.
     */
    public Scrim createScrim(ScrimCreateRequest dto, User organizador) throws IOException {
        List<Scrim> scrims = persistenceManager.readCollection(SCRIMS_FILE, Scrim.class);

        Scrim newScrim = new Scrim();
        newScrim.setId(UUID.randomUUID());
        newScrim.setOrganizadorId(organizador.getId());
        newScrim.setJuego(dto.getJuego());
        newScrim.setFormato(dto.getFormato());
        newScrim.setRegion(dto.getRegion());
        newScrim.setRangoMin(dto.getRangoMin());
        newScrim.setRangoMax(dto.getRangoMax());
        newScrim.setLatenciaMax(dto.getLatenciaMax());
        newScrim.setFechaHora(dto.getFechaHora());
        newScrim.setDuracion(dto.getDuracion());
        newScrim.setModalidad(dto.getModalidad());
        newScrim.setDescripcion(dto.getDescripcion());
        newScrim.setCupo(dto.getCupo());
        newScrim.setMatchmakingStrategyType(dto.getMatchmakingStrategyType());
        newScrim.setEstado(ScrimStateEnum.BUSCANDO);

        scrims.add(newScrim);
        persistenceManager.writeCollection(SCRIMS_FILE, scrims); // Guardar ANTES de publicar

        // --- PUBLICAR EVENTO ---
        eventBus.publish(new ScrimCreatedEvent(newScrim));
        logger.info("Scrim {} creado y evento ScrimCreatedEvent publicado.", newScrim.getId());

        return newScrim;
    }

    /**
     * Busca scrims con filtros opcionales.
     */
    public List<Scrim> findScrims(
            Optional<String> juego, Optional<String> region,
            Optional<String> rangoMin, Optional<String> rangoMax,
            Optional<Integer> latenciaMax) throws IOException {

        List<Scrim> scrims = persistenceManager.readCollection(SCRIMS_FILE, Scrim.class);
        Stream<Scrim> stream = scrims.stream();

        // Aplicar filtros
        if (juego.isPresent()) {
            String juegoFilter = juego.get();
            stream = stream.filter(s -> s.getJuego() != null && s.getJuego().equalsIgnoreCase(juegoFilter));
        }
        if (region.isPresent()) {
            String regionFilter = region.get();
            stream = stream.filter(s -> s.getRegion() != null && s.getRegion().equalsIgnoreCase(regionFilter));
        }
        if (latenciaMax.isPresent()) {
            Integer latenciaFilter = latenciaMax.get();
            stream = stream.filter(s -> s.getLatenciaMax() != null && s.getLatenciaMax() <= latenciaFilter);
        }
        if (rangoMin.isPresent()) {
            String rangoMinFilter = rangoMin.get();
             // Añadir chequeo null por si acaso
            stream = stream.filter(s -> s.getRangoMin() != null && s.getRangoMin().equalsIgnoreCase(rangoMinFilter));
        }
        if (rangoMax.isPresent()) {
            String rangoMaxFilter = rangoMax.get();
            // Añadir chequeo null por si acaso
            stream = stream.filter(s -> s.getRangoMax() != null && s.getRangoMax().equalsIgnoreCase(rangoMaxFilter));
        }

        // Devolvemos solo los que están buscando jugadores o en lobby
        return stream
                .filter(s -> s.getEstado() == ScrimStateEnum.BUSCANDO || s.getEstado() == ScrimStateEnum.LOBBY_ARMADO)
                .collect(Collectors.toList());
    }

    /**
     * Permite a un usuario postularse a un Scrim y PUBLICA LobbyArmadoEvent si se llena.
     */
    public Postulacion postularse(UUID scrimId, PostulacionRequest dto, User jugador) throws IOException {
        // --- Leer Scrim ---
        List<Scrim> currentScrims = persistenceManager.readCollection(SCRIMS_FILE, Scrim.class);
        Optional<Scrim> scrimOpt = currentScrims.stream().filter(s -> s.getId().equals(scrimId)).findFirst();
        if (scrimOpt.isEmpty()) {
            throw new ScrimNotFoundException("No se encontró el Scrim con ID: " + scrimId);
        }
        Scrim scrim = scrimOpt.get(); // Instancia de la lista

        // --- Validaciones ---
        if (scrim.getEstado() != ScrimStateEnum.BUSCANDO) {
            throw new InvalidScrimStateException("No te puedes postular a este scrim (Estado: " + scrim.getEstado() + ").");
        }
        List<Postulacion> postulaciones = persistenceManager.readCollection(POSTULACIONES_FILE, Postulacion.class);
        boolean yaPostulado = postulaciones.stream()
                .anyMatch(p -> p.getScrimId().equals(scrimId) && p.getUsuarioId().equals(jugador.getId()));
        if (yaPostulado) {
            throw new InvalidScrimStateException("Ya te has postulado a este scrim.");
        }
        if (scrim.getOrganizadorId().equals(jugador.getId())) {
             throw new InvalidScrimStateException("No te puedes postular a tu propio scrim.");
        }

        // --- Crear Postulación ---
        Postulacion newPostulacion = new Postulacion();
        newPostulacion.setId(UUID.randomUUID());
        newPostulacion.setScrimId(scrimId);
        newPostulacion.setUsuarioId(jugador.getId());
        newPostulacion.setRolDeseado(dto.getRolDeseado());
        newPostulacion.setLatenciaReportada(dto.getLatenciaReportada());
        newPostulacion.setFechaPostulacion(LocalDateTime.now());
        newPostulacion.setEstado(PostulacionState.PENDIENTE);

        // Añadir a la lista en memoria ANTES de contar
        postulaciones.add(newPostulacion);

        // --- Contar Postulantes ---
        long postulantesActivos = postulaciones.stream()
                .filter(p -> p.getScrimId().equals(scrimId) &&
                             (p.getEstado() == PostulacionState.PENDIENTE || p.getEstado() == PostulacionState.ACEPTADA))
                .count();

        // Verificar si se llenó el cupo
        boolean cupoLleno = (scrim.getCupo() != null) && (postulantesActivos + 1) >= scrim.getCupo();

        // --- Guardar Postulación PRIMERO ---
        persistenceManager.writeCollection(POSTULACIONES_FILE, postulaciones);
        logger.info("[EVENTO] Nueva postulación para Scrim {} por {}", scrim.getId(), jugador.getUsername());


        // --- Cambiar estado y PUBLICAR EVENTO si se llenó ---
        if (cupoLleno && scrim.getEstado() == ScrimStateEnum.BUSCANDO) {
            logger.info("[INFO] Cupo lleno para Scrim {}. Cambiando estado y publicando evento.", scrim.getId());

            // Actualizar el estado del objeto 'scrim' EN LA LISTA 'currentScrims'
            scrim.setEstado(ScrimStateEnum.LOBBY_ARMADO); // Actualiza la instancia que encontramos al principio

            try {
                // Guardar la lista COMPLETA de scrims actualizada
                persistenceManager.writeCollection(SCRIMS_FILE, currentScrims);
                logger.info("[EVENTO] Scrim cambió a LOBBY_ARMADO (Cupo Lleno): {}", scrim.getId());

                // --- PUBLICAR EVENTO LobbyArmado ---
                eventBus.publish(new LobbyArmadoEvent(scrim)); // Pasar el objeto scrim
                logger.info("Evento LobbyArmadoEvent publicado para Scrim {}", scrim.getId());

            } catch (IOException e) {
                 logger.error("CRITICAL: Error al guardar {} después de actualizar estado a LOBBY_ARMADO para Scrim {}. Estado podría estar inconsistente.", SCRIMS_FILE, scrimId, e);
                 scrim.setEstado(ScrimStateEnum.BUSCANDO); // Revertir estado en memoria
                 throw e;
            } catch (Exception e) {
                 logger.error("Error inesperado al publicar LobbyArmadoEvent para Scrim {}.", scrimId, e);
            }
        }

        return newPostulacion;
    }


    /**
     * Confirma la participación y PUBLICA ScrimConfirmadoEvent si todos confirman.
     */
    public void confirmar(UUID scrimId, User jugador) throws IOException {
        // --- 1. Validar estado del Scrim ---
        List<Scrim> currentScrims = persistenceManager.readCollection(SCRIMS_FILE, Scrim.class);
        Optional<Scrim> scrimOpt = currentScrims.stream().filter(s -> s.getId().equals(scrimId)).findFirst();
        if (scrimOpt.isEmpty()) {
            throw new ScrimNotFoundException("No se encontró el Scrim con ID: " + scrimId);
        }
        Scrim scrim = scrimOpt.get(); // Instancia de la lista

        if (scrim.getEstado() != ScrimStateEnum.LOBBY_ARMADO) {
            throw new InvalidScrimStateException("No se puede confirmar participación (Estado del Scrim: " + scrim.getEstado() + ").");
        }

        // --- 2. Actualizar la Postulación ---
        List<Postulacion> currentPostulaciones = persistenceManager.readCollection(POSTULACIONES_FILE, Postulacion.class);
        Optional<Postulacion> postulacionOpt = currentPostulaciones.stream()
                .filter(p -> p.getScrimId().equals(scrimId) && p.getUsuarioId().equals(jugador.getId()))
                .findFirst();

        if (postulacionOpt.isEmpty()) {
            throw new InvalidScrimStateException("No se encontró tu postulación para este scrim.");
        }
        Postulacion postulacion = postulacionOpt.get(); // Instancia de la lista

        if (postulacion.getEstado() == PostulacionState.RECHAZADA) {
            throw new InvalidScrimStateException("Tu postulación fue rechazada previamente.");
        }

        boolean wasAlreadyAccepted = postulacion.getEstado() == PostulacionState.ACEPTADA;
        if (!wasAlreadyAccepted) {
            postulacion.setEstado(PostulacionState.ACEPTADA); // Actualizar en la instancia de la lista
            persistenceManager.writeCollection(POSTULACIONES_FILE, currentPostulaciones); // Guardar cambio
            logger.info("[EVENTO] Jugador {} confirmó (Postulación ACEPTADA) para Scrim {}", jugador.getUsername(), scrimId);
            // eventBus.publish(new PostulacionAceptadaEvent(postulacion));
        } else {
             logger.info("Jugador {} ya había confirmado para Scrim {}", jugador.getUsername(), scrimId);
        }


        // --- 3. Verificar si todos confirmaron y PUBLICAR EVENTO ---
        long aceptados = currentPostulaciones.stream()
                .filter(p -> p.getScrimId().equals(scrimId) && p.getEstado() == PostulacionState.ACEPTADA)
                .count();

        boolean todosConfirmados = (scrim.getCupo() != null) && (aceptados + 1) >= scrim.getCupo();

        if (todosConfirmados && scrim.getEstado() == ScrimStateEnum.LOBBY_ARMADO) {
            // Actualizar estado del scrim en la lista 'currentScrims'
            scrim.setEstado(ScrimStateEnum.CONFIRMADO);
            try {
                persistenceManager.writeCollection(SCRIMS_FILE, currentScrims); // Guardar estado actualizado
                logger.info("[EVENTO] ¡Todos confirmaron! Scrim cambió a CONFIRMADO: {}", scrim.getId());

                // --- PUBLICAR EVENTO ScrimConfirmado ---
                eventBus.publish(new ScrimConfirmadoEvent(scrim)); // Pasar el objeto scrim
                logger.info("Evento ScrimConfirmadoEvent publicado para Scrim {}", scrim.getId());

            } catch (IOException e) {
                 logger.error("CRITICAL: Error al guardar {} después de actualizar estado a CONFIRMADO para Scrim {}.", SCRIMS_FILE, scrimId, e);
                 scrim.setEstado(ScrimStateEnum.LOBBY_ARMADO); // Revertir
                 throw e;
            } catch (Exception e) {
                logger.error("Error inesperado al publicar ScrimConfirmadoEvent para Scrim {}", scrimId, e);
            }
        } else if (!wasAlreadyAccepted) {
            logger.info("Aún faltan confirmaciones para Scrim {}. Aceptados: {}/{}", scrimId, aceptados, (scrim.getCupo() != null ? scrim.getCupo() - 1 : "?"));
        }
    }


    /**
     * Inicia manualmente un Scrim y PUBLICA ScrimIniciadoEvent.
     * --- MÉTODO MODIFICADO ---
     */
    public void iniciarScrim(UUID scrimId, User organizador) throws IOException {
        List<Scrim> currentScrims = persistenceManager.readCollection(SCRIMS_FILE, Scrim.class);
        Optional<Scrim> scrimOpt = currentScrims.stream().filter(s -> s.getId().equals(scrimId)).findFirst();
         if (scrimOpt.isEmpty()) {
             throw new ScrimNotFoundException("No se encontró el Scrim con ID: " + scrimId);
         }
        Scrim scrim = scrimOpt.get(); // Instancia de la lista

        if (!scrim.getOrganizadorId().equals(organizador.getId())) {
            throw new UnauthorizedException("Solo el organizador puede iniciar el scrim.");
        }
        if (scrim.getEstado() != ScrimStateEnum.CONFIRMADO) {
            throw new InvalidScrimStateException("Solo se puede iniciar un scrim que esté 'Confirmado' (Estado: " + scrim.getEstado() + ").");
        }

        // Guardar estado anterior
        ScrimStateEnum estadoAnterior = scrim.getEstado();
        // Actualizar el estado en la lista
        scrim.setEstado(ScrimStateEnum.EN_JUEGO);
        try {
            persistenceManager.writeCollection(SCRIMS_FILE, currentScrims); // Guardar
            logger.info("[EVENTO] Scrim Iniciado: {}", scrim.getId());

            // --- PUBLICAR EVENTO ScrimIniciado ---
            eventBus.publish(new ScrimIniciadoEvent(scrim));
            logger.info("Evento ScrimIniciadoEvent publicado para Scrim {}", scrim.getId());

        } catch (IOException e) {
             logger.error("CRITICAL: Error al guardar {} después de actualizar estado a EN_JUEGO para Scrim {}.", SCRIMS_FILE, scrimId, e);
             scrim.setEstado(estadoAnterior); // Revertir
             throw e;
        } catch (Exception e) {
            logger.error("Error inesperado al publicar ScrimIniciadoEvent para Scrim {}", scrimId, e);
        }
    }


    /**
     * Cancela un Scrim y PUBLICA ScrimCanceladoEvent.
     * --- MÉTODO MODIFICADO ---
     */
    public void cancelarScrim(UUID scrimId, User organizador) throws IOException {
        List<Scrim> currentScrims = persistenceManager.readCollection(SCRIMS_FILE, Scrim.class);
        Optional<Scrim> scrimOpt = currentScrims.stream().filter(s -> s.getId().equals(scrimId)).findFirst();
         if (scrimOpt.isEmpty()) {
             throw new ScrimNotFoundException("No se encontró el Scrim con ID: " + scrimId);
         }
        Scrim scrim = scrimOpt.get(); // Instancia de la lista

        if (!scrim.getOrganizadorId().equals(organizador.getId())) {
            throw new UnauthorizedException("Solo el organizador puede cancelar el scrim.");
        }
        if (scrim.getEstado() == ScrimStateEnum.EN_JUEGO ||
            scrim.getEstado() == ScrimStateEnum.FINALIZADO ||
            scrim.getEstado() == ScrimStateEnum.CANCELADO) {
            throw new InvalidScrimStateException("No se puede cancelar un scrim en este estado (Estado: " + scrim.getEstado() + ").");
        }

        ScrimStateEnum estadoAnterior = scrim.getEstado();
        scrim.setEstado(ScrimStateEnum.CANCELADO); // Actualizar estado en la lista
        try {
             persistenceManager.writeCollection(SCRIMS_FILE, currentScrims); // Guardar
             logger.info("[EVENTO] Scrim Cancelado: {}", scrim.getId());

             // --- PUBLICAR EVENTO ScrimCancelado ---
             eventBus.publish(new ScrimCanceladoEvent(scrim));
             logger.info("Evento ScrimCanceladoEvent publicado para Scrim {}", scrim.getId());
             // La notificación a participantes se hará en el Subscriber

        } catch (IOException e) {
             logger.error("CRITICAL: Error al guardar {} después de actualizar estado a CANCELADO para Scrim {}.", SCRIMS_FILE, scrimId, e);
             scrim.setEstado(estadoAnterior); // Revertir
             throw e;
        } catch (Exception e) {
             logger.error("Error inesperado al publicar ScrimCanceladoEvent para Scrim {}", scrimId, e);
        }
    }

    /**
     * Finaliza un Scrim y PUBLICA ScrimFinalizadoEvent.
     * --- MÉTODO MODIFICADO ---
     */
    public void finalizarScrim(UUID scrimId, User organizador) throws IOException {
        List<Scrim> currentScrims = persistenceManager.readCollection(SCRIMS_FILE, Scrim.class);
        Optional<Scrim> scrimOpt = currentScrims.stream().filter(s -> s.getId().equals(scrimId)).findFirst();
         if (scrimOpt.isEmpty()) {
             throw new ScrimNotFoundException("No se encontró el Scrim con ID: " + scrimId);
         }
        Scrim scrim = scrimOpt.get(); // Instancia de la lista

        if (!scrim.getOrganizadorId().equals(organizador.getId())) {
            throw new UnauthorizedException("Solo el organizador puede finalizar el scrim manualmente.");
        }
        if (scrim.getEstado() != ScrimStateEnum.EN_JUEGO && scrim.getEstado() != ScrimStateEnum.CONFIRMADO) {
             throw new InvalidScrimStateException("Solo se puede finalizar un scrim que esté 'En Juego' o 'Confirmado' (Estado: " + scrim.getEstado() + ").");
        }

        ScrimStateEnum estadoAnterior = scrim.getEstado();
        scrim.setEstado(ScrimStateEnum.FINALIZADO); // Actualizar estado en la lista
        try {
            persistenceManager.writeCollection(SCRIMS_FILE, currentScrims); // Guardar
            logger.info("[EVENTO] Scrim Finalizado: {}", scrim.getId());

            // --- PUBLICAR EVENTO ScrimFinalizado ---
            eventBus.publish(new ScrimFinalizadoEvent(scrim));
            logger.info("Evento ScrimFinalizadoEvent publicado para Scrim {}", scrim.getId());

        } catch (IOException e) {
             logger.error("CRITICAL: Error al guardar {} después de actualizar estado a FINALIZADO para Scrim {}.", SCRIMS_FILE, scrimId, e);
             scrim.setEstado(estadoAnterior); // Revertir
             throw e;
        } catch (Exception e) {
             logger.error("Error inesperado al publicar ScrimFinalizadoEvent para Scrim {}", scrimId, e);
        }
    }

    /**
     * Guarda las estadísticas para un Scrim finalizado.
     */
    public void guardarEstadisticas(UUID scrimId, List<EstadisticaRequest> statsRequests, User organizador) throws IOException {
        // ... (sin cambios respecto a la versión anterior) ...
        List<Scrim> scrims = persistenceManager.readCollection(SCRIMS_FILE, Scrim.class);
        Scrim scrim = scrims.stream().filter(s -> s.getId().equals(scrimId)).findFirst()
                .orElseThrow(() -> new ScrimNotFoundException("No se encontró el Scrim con ID: " + scrimId));

        if (!scrim.getOrganizadorId().equals(organizador.getId())) {
            throw new UnauthorizedException("Solo el organizador puede subir las estadísticas del scrim.");
        }
        if (scrim.getEstado() != ScrimStateEnum.FINALIZADO) {
            throw new InvalidScrimStateException("Solo se pueden subir estadísticas para un scrim 'Finalizado' (Estado: " + scrim.getEstado() + ").");
        }

        List<Estadistica> allStats = persistenceManager.readCollection(ESTADISTICAS_FILE, Estadistica.class);
        boolean alreadyExists = allStats.stream().anyMatch(s -> s.getScrimId().equals(scrimId));
        if (alreadyExists) {
             logger.warn("[WARN] Ya existen estadísticas para el Scrim: {}. No se guardarán de nuevo.", scrimId);
             return;
        }

        List<Estadistica> newStats = new ArrayList<>();
        for (EstadisticaRequest req : statsRequests) {
             if (req.getUsuarioId() == null || req.getKills() == null || req.getDeaths() == null || req.getAssists() == null) {
                 logger.warn("Se omitió una entrada de estadística para el scrim {} debido a campos nulos.", scrimId);
                 continue;
             }
            Estadistica stat = new Estadistica();
            stat.setId(UUID.randomUUID());
            stat.setScrimId(scrimId);
            stat.setUsuarioId(req.getUsuarioId());
            stat.setMvp(req.isMvp());
            stat.setKills(req.getKills());
            stat.setDeaths(req.getDeaths());
            stat.setAssists(req.getAssists());
            stat.setObservaciones(req.getObservaciones());
            newStats.add(stat);
        }

        if (!newStats.isEmpty()) {
            allStats.addAll(newStats);
            persistenceManager.writeCollection(ESTADISTICAS_FILE, allStats);
            logger.info("[INFO] {} nuevas estadísticas guardadas para Scrim: {}", newStats.size(), scrim.getId());
        } else {
             logger.warn("[WARN] No se guardaron estadísticas para Scrim {} (lista vacía o datos inválidos).", scrimId);
        }
    }
}

