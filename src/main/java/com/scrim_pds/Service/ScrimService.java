package com.scrim_pds.service;

import com.scrim_pds.dto.EstadisticaRequest;
import com.scrim_pds.dto.PostulacionRequest;
import com.scrim_pds.dto.ScrimCreateRequest;
import com.scrim_pds.event.*;
import com.scrim_pds.exception.InvalidScrimStateException;
import com.scrim_pds.exception.ScrimNotFoundException;
import com.scrim_pds.exception.UnauthorizedException;
import com.scrim_pds.model.Estadistica;
import com.scrim_pds.model.Postulacion;
import com.scrim_pds.model.Scrim;
import com.scrim_pds.model.User;
import com.scrim_pds.model.enums.Formato;
import com.scrim_pds.model.enums.PostulacionState;
import com.scrim_pds.model.enums.ScrimStateEnum;
import com.scrim_pds.persistence.JsonPersistenceManager;
import com.scrim_pds.service.UserService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate; // Importar
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
        newScrim.setRecordatorioEnviado(false); // Asegurar default

        scrims.add(newScrim);
        persistenceManager.writeCollection(SCRIMS_FILE, scrims);

        // --- LOG DE AUDITORIA ---
        logger.info("[AUDIT] Usuario '{}' (ID: {}) creó Scrim '{}'", organizador.getUsername(), organizador.getId(), newScrim.getId());
        
        eventBus.publish(new ScrimCreatedEvent(newScrim));
        logger.info("Evento ScrimCreatedEvent publicado para Scrim {}.", newScrim.getId()); // Log de evento

        return newScrim;
    }

    // --- NUEVO MÉTODO AÑADIDO PARA iCal ---
    /**
     * Busca un Scrim por su ID.
     * @param scrimId El ID del Scrim.
     * @return El Scrim encontrado.
     * @throws ScrimNotFoundException Si no se encuentra.
     */
    public Scrim findScrimById(UUID scrimId) throws IOException {
        // Usamos un ReadLock
        List<Scrim> scrims = persistenceManager.readCollection(SCRIMS_FILE, Scrim.class);
        return scrims.stream()
                .filter(s -> s.getId().equals(scrimId))
                .findFirst()
                .orElseThrow(() -> new ScrimNotFoundException("No se encontró el Scrim con ID: " + scrimId));
    }


    /**
     * Busca scrims con filtros opcionales.
     * --- MÉTODO MODIFICADO (7 PARÁMETROS) ---
     */
    public List<Scrim> findScrims(
            Optional<String> juego, Optional<String> region,
            Optional<String> rangoMin, Optional<String> rangoMax,
            Optional<Integer> latenciaMax,
            // --- NUEVOS PARÁMETROS ---
            Optional<Formato> formato,
            Optional<LocalDate> fecha) throws IOException {

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
            stream = stream.filter(s -> s.getRangoMin() != null && s.getRangoMin().equalsIgnoreCase(rangoMinFilter));
        }
        if (rangoMax.isPresent()) {
            String rangoMaxFilter = rangoMax.get();
            stream = stream.filter(s -> s.getRangoMax() != null && s.getRangoMax().equalsIgnoreCase(rangoMaxFilter));
        }
        
        // --- NUEVOS FILTROS ---
        if (formato.isPresent()) {
            Formato formatoFilter = formato.get();
            stream = stream.filter(s -> s.getFormato() != null && s.getFormato() == formatoFilter);
        }
        if (fecha.isPresent()) {
            LocalDate fechaFilter = fecha.get();
            stream = stream.filter(s -> s.getFechaHora() != null &&
                                        s.getFechaHora().toLocalDate().isEqual(fechaFilter));
        }
        // --- FIN NUEVOS FILTROS ---

        return stream
                .filter(s -> s.getEstado() == ScrimStateEnum.BUSCANDO || s.getEstado() == ScrimStateEnum.LOBBY_ARMADO)
                .collect(Collectors.toList());
    }

    /**
     * Permite a un usuario postularse a un Scrim y PUBLICA LobbyArmadoEvent si se llena.
     */
    public Postulacion postularse(UUID scrimId, PostulacionRequest dto, User jugador) throws IOException {
        List<Scrim> currentScrims = persistenceManager.readCollection(SCRIMS_FILE, Scrim.class);
        Optional<Scrim> scrimOpt = currentScrims.stream().filter(s -> s.getId().equals(scrimId)).findFirst();
        if (scrimOpt.isEmpty()) { /* ... */ throw new ScrimNotFoundException("No se encontró el Scrim con ID: " + scrimId); }
        Scrim scrim = scrimOpt.get();
        if (scrim.getEstado() != ScrimStateEnum.BUSCANDO) { /* ... */ throw new InvalidScrimStateException("No te puedes postular..."); }
        List<Postulacion> postulaciones = persistenceManager.readCollection(POSTULACIONES_FILE, Postulacion.class);
        boolean yaPostulado = postulaciones.stream().anyMatch(p -> p.getScrimId().equals(scrimId) && p.getUsuarioId().equals(jugador.getId()));
        if (yaPostulado) { /* ... */ throw new InvalidScrimStateException("Ya te has postulado..."); }
        if (scrim.getOrganizadorId().equals(jugador.getId())) { /* ... */ throw new InvalidScrimStateException("No te puedes postular..."); }
        
        Postulacion newPostulacion = new Postulacion();
        // ... (setear campos postulación) ...
        newPostulacion.setId(UUID.randomUUID());
        newPostulacion.setScrimId(scrimId);
        newPostulacion.setUsuarioId(jugador.getId());
        newPostulacion.setRolDeseado(dto.getRolDeseado());
        newPostulacion.setLatenciaReportada(dto.getLatenciaReportada());
        newPostulacion.setFechaPostulacion(LocalDateTime.now());
        newPostulacion.setEstado(PostulacionState.PENDIENTE);
        
        postulaciones.add(newPostulacion);
        
        long postulantesActivos = postulaciones.stream()
                .filter(p -> p.getScrimId().equals(scrimId) && (p.getEstado() == PostulacionState.PENDIENTE || p.getEstado() == PostulacionState.ACEPTADA))
                .count();
        boolean cupoLleno = (scrim.getCupo() != null) && (postulantesActivos + 1) >= scrim.getCupo();
        
        persistenceManager.writeCollection(POSTULACIONES_FILE, postulaciones);
        logger.info("[EVENTO] Nueva postulación para Scrim {} por {}", scrim.getId(), jugador.getUsername());
        
        if (cupoLleno && scrim.getEstado() == ScrimStateEnum.BUSCANDO) {
            logger.info("[INFO] Cupo lleno para Scrim {}. Cambiando estado y publicando evento.", scrim.getId());
            scrim.setEstado(ScrimStateEnum.LOBBY_ARMADO);
            try {
                persistenceManager.writeCollection(SCRIMS_FILE, currentScrims);
                logger.info("[EVENTO] Scrim cambió a LOBBY_ARMADO (Cupo Lleno): {}", scrim.getId());
                eventBus.publish(new LobbyArmadoEvent(scrim));
                logger.info("Evento LobbyArmadoEvent publicado para Scrim {}", scrim.getId());
            } catch (IOException e) { /* ... */ scrim.setEstado(ScrimStateEnum.BUSCANDO); throw e; }
              catch (Exception e) { /* ... */ }
        }
        return newPostulacion;
    }


    /**
     * Confirma la participación y PUBLICA ScrimConfirmadoEvent si todos confirman.
     */
    public void confirmar(UUID scrimId, User jugador) throws IOException {
        List<Scrim> currentScrims = persistenceManager.readCollection(SCRIMS_FILE, Scrim.class);
        Optional<Scrim> scrimOpt = currentScrims.stream().filter(s -> s.getId().equals(scrimId)).findFirst();
        if (scrimOpt.isEmpty()) { /* ... */ throw new ScrimNotFoundException("No se encontró el Scrim con ID: " + scrimId); }
        Scrim scrim = scrimOpt.get();
        if (scrim.getEstado() != ScrimStateEnum.LOBBY_ARMADO) { /* ... */ throw new InvalidScrimStateException("No se puede confirmar..."); }
        
        List<Postulacion> currentPostulaciones = persistenceManager.readCollection(POSTULACIONES_FILE, Postulacion.class);
        Optional<Postulacion> postulacionOpt = currentPostulaciones.stream().filter(p -> p.getScrimId().equals(scrimId) && p.getUsuarioId().equals(jugador.getId())).findFirst();
        if (postulacionOpt.isEmpty()) { /* ... */ throw new InvalidScrimStateException("No se encontró tu postulación..."); }
        Postulacion postulacion = postulacionOpt.get();
        if (postulacion.getEstado() == PostulacionState.RECHAZADA) { /* ... */ throw new InvalidScrimStateException("Tu postulación fue rechazada..."); }
        
        boolean wasAlreadyAccepted = postulacion.getEstado() == PostulacionState.ACEPTADA;
        if (!wasAlreadyAccepted) {
            postulacion.setEstado(PostulacionState.ACEPTADA);
            persistenceManager.writeCollection(POSTULACIONES_FILE, currentPostulaciones);
            logger.info("[EVENTO] Jugador {} confirmó (Postulación ACEPTADA) para Scrim {}", jugador.getUsername(), scrimId);
        } else {
             logger.info("Jugador {} ya había confirmado para Scrim {}", jugador.getUsername(), scrimId);
        }

        long aceptados = currentPostulaciones.stream().filter(p -> p.getScrimId().equals(scrimId) && p.getEstado() == PostulacionState.ACEPTADA).count();
        boolean todosConfirmados = (scrim.getCupo() != null) && (aceptados + 1) >= scrim.getCupo();

        if (todosConfirmados && scrim.getEstado() == ScrimStateEnum.LOBBY_ARMADO) {
            scrim.setEstado(ScrimStateEnum.CONFIRMADO);
            try {
                persistenceManager.writeCollection(SCRIMS_FILE, currentScrims);
                logger.info("[EVENTO] ¡Todos confirmaron! Scrim cambió a CONFIRMADO: {}", scrim.getId());
                eventBus.publish(new ScrimConfirmadoEvent(scrim));
                logger.info("Evento ScrimConfirmadoEvent publicado para Scrim {}", scrim.getId());
            } catch (IOException e) { /* ... */ scrim.setEstado(ScrimStateEnum.LOBBY_ARMADO); throw e; }
              catch (Exception e) { /* ... */ }
        } else if (!wasAlreadyAccepted) {
            logger.info("Aún faltan confirmaciones para Scrim {}. Aceptados: {}/{}", scrimId, aceptados, (scrim.getCupo() != null ? scrim.getCupo() - 1 : "?"));
        }
    }


    /**
     * Inicia manualmente un Scrim y PUBLICA ScrimIniciadoEvent.
     */
    public void iniciarScrim(UUID scrimId, @Nullable User actor) throws IOException {
        List<Scrim> currentScrims = persistenceManager.readCollection(SCRIMS_FILE, Scrim.class);
        Optional<Scrim> scrimOpt = currentScrims.stream().filter(s -> s.getId().equals(scrimId)).findFirst();
         
        if (scrimOpt.isEmpty()) {
             if (actor != null) { throw new ScrimNotFoundException("No se encontró el Scrim con ID: " + scrimId); }
             logger.warn("[Scheduler] Scrim {} no encontrado para iniciar (¿ya fue borrado?).", scrimId);
             return;
         }
        Scrim scrim = scrimOpt.get();
        if (actor != null && !scrim.getOrganizadorId().equals(actor.getId())) {
            throw new UnauthorizedException("Solo el organizador puede iniciar el scrim.");
        }
        if (scrim.getEstado() != ScrimStateEnum.CONFIRMADO) {
            String actorName = actor != null ? actor.getUsername() : "Scheduler";
            logger.debug("[{}] Scrim {} ya no está CONFIRMADO (Estado: {}). Omitiendo inicio.", actorName, scrimId, scrim.getEstado());
            if (actor != null) {
                 throw new InvalidScrimStateException("Solo se puede iniciar un scrim que esté 'Confirmado' (Estado: " + scrim.getEstado() + ").");
            }
            return;
        }

        ScrimStateEnum estadoAnterior = scrim.getEstado();
        scrim.setEstado(ScrimStateEnum.EN_JUEGO);
        try {
            persistenceManager.writeCollection(SCRIMS_FILE, currentScrims);
            String actorName = actor != null ? actor.getUsername() : "SISTEMA";
            logger.info("[AUDIT] Scrim {} fue INICIADO por Actor: {}", scrim.getId(), actorName);
            eventBus.publish(new ScrimIniciadoEvent(scrim));
            logger.info("Evento ScrimIniciadoEvent publicado para Scrim {}", scrim.getId());
        } catch (IOException e) { /* ... */ scrim.setEstado(estadoAnterior); throw e; }
          catch (Exception e) { /* ... */ }
    }


    /**
     * Cancela un Scrim y PUBLICA ScrimCanceladoEvent.
     */
    public void cancelarScrim(UUID scrimId, User organizador) throws IOException {
        List<Scrim> currentScrims = persistenceManager.readCollection(SCRIMS_FILE, Scrim.class);
        Optional<Scrim> scrimOpt = currentScrims.stream().filter(s -> s.getId().equals(scrimId)).findFirst();
         if (scrimOpt.isEmpty()) { /* ... */ throw new ScrimNotFoundException("No se encontró el Scrim..."); }
        Scrim scrim = scrimOpt.get();
        if (!scrim.getOrganizadorId().equals(organizador.getId())) { /* ... */ throw new UnauthorizedException("Solo el organizador..."); }
        if (scrim.getEstado() == ScrimStateEnum.EN_JUEGO ||
            scrim.getEstado() == ScrimStateEnum.FINALIZADO ||
            scrim.getEstado() == ScrimStateEnum.CANCELADO) {
            throw new InvalidScrimStateException("No se puede cancelar un scrim en este estado...");
        }

        ScrimStateEnum estadoAnterior = scrim.getEstado();
        scrim.setEstado(ScrimStateEnum.CANCELADO);
        try {
             persistenceManager.writeCollection(SCRIMS_FILE, currentScrims);
             logger.info("[AUDIT] Usuario '{}' (ID: {}) canceló Scrim '{}'", organizador.getUsername(), organizador.getId(), scrim.getId());
             eventBus.publish(new ScrimCanceladoEvent(scrim));
             logger.info("Evento ScrimCanceladoEvent publicado para Scrim {}", scrim.getId());
        } catch (IOException e) { /* ... */ scrim.setEstado(estadoAnterior); throw e; }
          catch (Exception e) { /* ... */ }
    }

    /**
     * Finaliza un Scrim y PUBLICA ScrimFinalizadoEvent.
     */
    public void finalizarScrim(UUID scrimId, User organizador) throws IOException {
         List<Scrim> currentScrims = persistenceManager.readCollection(SCRIMS_FILE, Scrim.class);
        Optional<Scrim> scrimOpt = currentScrims.stream().filter(s -> s.getId().equals(scrimId)).findFirst();
         if (scrimOpt.isEmpty()) { /* ... */ throw new ScrimNotFoundException("No se encontró el Scrim..."); }
        Scrim scrim = scrimOpt.get();
        if (!scrim.getOrganizadorId().equals(organizador.getId())) { /* ... */ throw new UnauthorizedException("Solo el organizador..."); }
        if (scrim.getEstado() != ScrimStateEnum.EN_JUEGO && scrim.getEstado() != ScrimStateEnum.CONFIRMADO) { /* ... */ throw new InvalidScrimStateException("Solo se puede finalizar...");}
        
        ScrimStateEnum estadoAnterior = scrim.getEstado();
        scrim.setEstado(ScrimStateEnum.FINALIZADO);
        try {
            persistenceManager.writeCollection(SCRIMS_FILE, currentScrims);
            logger.info("[AUDIT] Usuario '{}' (ID: {}) finalizó Scrim '{}'", organizador.getUsername(), organizador.getId(), scrim.getId());
            eventBus.publish(new ScrimFinalizadoEvent(scrim));
            logger.info("Evento ScrimFinalizadoEvent publicado para Scrim {}", scrim.getId());
        } catch (IOException e) { /* ... */ scrim.setEstado(estadoAnterior); throw e; }
          catch (Exception e) { /* ... */ }
    }

    /**
     * Guarda las estadísticas para un Scrim finalizado.
     */
    public void guardarEstadisticas(UUID scrimId, List<EstadisticaRequest> statsRequests, User organizador) throws IOException {
        List<Scrim> scrims = persistenceManager.readCollection(SCRIMS_FILE, Scrim.class);
        Scrim scrim = scrims.stream().filter(s -> s.getId().equals(scrimId)).findFirst()
                .orElseThrow(() -> new ScrimNotFoundException("No se encontró el Scrim con ID: " + scrimId));

        if (!scrim.getOrganizadorId().equals(organizador.getId())) { /* ... */ throw new UnauthorizedException("Solo el organizador..."); }
        if (scrim.getEstado() != ScrimStateEnum.FINALIZADO) { /* ... */ throw new InvalidScrimStateException("Solo se pueden subir stats..."); }

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
            // ... (copiar datos) ...
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
            logger.info("[AUDIT] Usuario '{}' (ID: {}) guardó {} entradas de estadísticas para Scrim '{}'", 
                        organizador.getUsername(), organizador.getId(), newStats.size(), scrimId);
        } else {
             logger.warn("[WARN] No se guardaron estadísticas para Scrim {} (lista vacía o datos inválidos).", scrimId);
        }
    }


    // --- MÉTODOS DEL SCHEDULER ---
    
    public List<Scrim> findScrimsToAutoStart() throws IOException {
        List<Scrim> currentScrims = persistenceManager.readCollection(SCRIMS_FILE, Scrim.class);
        LocalDateTime now = LocalDateTime.now();
        return currentScrims.stream()
                .filter(s -> s.getEstado() == ScrimStateEnum.CONFIRMADO &&
                             s.getFechaHora() != null &&
                             s.getFechaHora().isBefore(now))
                .collect(Collectors.toList());
    }

    /**
     * Busca scrims confirmados que están por empezar (para enviar recordatorios)
     * Y QUE NO HAN SIDO NOTIFICADOS.
     * --- MÉTODO MODIFICADO ---
     */
    public List<Scrim> findScrimsForReminder(int soon_hours) throws IOException { // <-- Firma corregida (1 parámetro)
        List<Scrim> currentScrims = persistenceManager.readCollection(SCRIMS_FILE, Scrim.class);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderWindowEnd = now.plusHours(soon_hours);
        return currentScrims.stream()
                .filter(s -> s.getEstado() == ScrimStateEnum.CONFIRMADO &&
                             !s.isRecordatorioEnviado() && // <-- Lógica corregida
                             s.getFechaHora() != null &&
                             s.getFechaHora().isAfter(now) &&
                             s.getFechaHora().isBefore(reminderWindowEnd))
                .collect(Collectors.toList());
    }

    /**
     * Busca todos los participantes de un Scrim (Organizador + Aceptados).
     */
    public List<User> findParticipantsForScrim(UUID scrimId, UUID organizadorId) throws IOException {
        List<User> participants = new ArrayList<>();
        userService.findUserById(organizadorId).ifPresent(participants::add);
        List<Postulacion> postulaciones = persistenceManager.readCollection(POSTULACIONES_FILE, Postulacion.class);
        postulaciones.stream()
                .filter(p -> p.getScrimId().equals(scrimId) && p.getEstado() == PostulacionState.ACEPTADA)
                .forEach(p -> userService.findUserById(p.getUsuarioId()).ifPresent(participants::add));
        return participants.stream().distinct().collect(Collectors.toList());
    }
    
    /**
     * Marca un scrim como "recordatorio enviado" para evitar spam.
     * --- MÉTODO AÑADIDO ---
     */
    public void marcarRecordatorioComoEnviado(UUID scrimId) throws IOException {
        List<Scrim> currentScrims = persistenceManager.readCollection(SCRIMS_FILE, Scrim.class);
        Optional<Scrim> scrimOpt = currentScrims.stream().filter(s -> s.getId().equals(scrimId)).findFirst();
        if (scrimOpt.isPresent()) {
            Scrim scrim = scrimOpt.get();
            if (!scrim.isRecordatorioEnviado()) {
                scrim.setRecordatorioEnviado(true);
                try {
                    persistenceManager.writeCollection(SCRIMS_FILE, currentScrims);
                    logger.info("[Scheduler] Flag 'recordatorioEnviado' seteado para Scrim {}", scrimId);
                } catch (IOException e) {
                     logger.error("[Scheduler] CRITICAL: Error al guardar flag de recordatorio para Scrim {}. Podrían enviarse duplicados.", scrimId, e);
                     throw e;
                }
            } else {
                 logger.warn("[Scheduler] Scrim {} ya estaba marcado como 'recordatorioEnviado'. Omitiendo.", scrimId);
            }
        } else {
            logger.warn("[Scheduler] No se pudo encontrar Scrim {} para marcar recordatorio (¿fue borrado?).", scrimId);
        }
    }
}

