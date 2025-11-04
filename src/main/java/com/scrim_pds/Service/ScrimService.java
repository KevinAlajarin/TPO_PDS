package com.scrim_pds.service;

import com.scrim_pds.dto.*;
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
import java.util.Set;
import java.util.Comparator;

import com.scrim_pds.dto.PostulacionResponse; // <-- 1. Importar el DTO nuevo
import com.scrim_pds.model.enums.PostulacionState; // <-- 2. Importar el Enum
import com.scrim_pds.exception.UnauthorizedException; // <-- 3. Importar excepción

import java.util.ArrayList; // <-- 4. Asegúrate de tener estos imports
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Comparator;
import com.scrim_pds.dto.MyScrimResponse;
import com.scrim_pds.model.enums.PostulacionState;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Comparator;

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
     *
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
        boolean cupoLleno = (scrim.getCupo() != null) && (postulantesActivos >= scrim.getCupo()); // sin +1
        
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
        Scrim scrim = currentScrims.stream().filter(s -> s.getId().equals(scrimId)).findFirst()
                .orElseThrow(() -> new ScrimNotFoundException("No se encontró el Scrim con ID: " + scrimId));

        if (scrim.getEstado() != ScrimStateEnum.LOBBY_ARMADO) {
            throw new InvalidScrimStateException("Solo se puede confirmar en estado LOBBY_ARMADO.");
        }

        List<Postulacion> all = persistenceManager.readCollection(POSTULACIONES_FILE, Postulacion.class);
        Postulacion myPost = all.stream()
                .filter(p -> p.getScrimId().equals(scrimId) && p.getUsuarioId().equals(jugador.getId()))
                .findFirst()
                .orElseThrow(() -> new InvalidScrimStateException("No se encontró tu postulación para este scrim."));

        if (myPost.getEstado() != PostulacionState.ACEPTADA) {
            throw new InvalidScrimStateException("Solo pueden confirmar quienes fueron ACEPTADOS por el organizador.");
        }

        if (!myPost.getHasConfirmed()) {
            myPost.setHasConfirmed(true);                     // <-- marcar confirmación del jugador
            persistenceManager.writeCollection(POSTULACIONES_FILE, all);
            logger.info("[EVENTO] Jugador {} confirmó asistencia (hasConfirmed=true) para Scrim {}", jugador.getUsername(), scrimId);
        } else {
            logger.info("Jugador {} ya había confirmado para Scrim {}", jugador.getUsername(), scrimId);
        }

        // ¿Todos los aceptados confirmaron?
        long aceptadasYConfirmadas = all.stream()
                .filter(p -> p.getScrimId().equals(scrimId))
                .filter(p -> p.getEstado() == PostulacionState.ACEPTADA && p.getHasConfirmed())
                .count();

        Integer cupo = scrim.getCupo();
        boolean todosConfirmados = (cupo != null) && (aceptadasYConfirmadas >= cupo); // sin +1

        if (todosConfirmados && scrim.getEstado() == ScrimStateEnum.LOBBY_ARMADO) {
            scrim.setEstado(ScrimStateEnum.CONFIRMADO);
            persistenceManager.writeCollection(SCRIMS_FILE, currentScrims);
            eventBus.publish(new ScrimConfirmadoEvent(scrim));
            logger.info("Scrim {} cambió a CONFIRMADO (todos los aceptados confirmaron).", scrim.getId());
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
    public List<MyScrimResponse> findMyScrims(User user) throws IOException {
        List<Scrim> allScrims = persistenceManager.readCollection(SCRIMS_FILE, Scrim.class);
        List<Postulacion> allPostulaciones = persistenceManager.readCollection(POSTULACIONES_FILE, Postulacion.class);

        // 1. Crear un Mapa de (ScrimID -> EstadoPostulacion) para este usuario
        Map<UUID, PostulacionState> postulacionesMap = allPostulaciones.stream()
                .filter(p -> p.getUsuarioId().equals(user.getId()))
                .collect(Collectors.toMap(
                        Postulacion::getScrimId,
                        Postulacion::getEstado,
                        (estadoExistente, estadoNuevo) -> estadoExistente // Manejo de duplicados
                ));

        // 2. Filtrar la lista completa de Scrims y crear el DTO
        return allScrims.stream()
                .filter(scrim ->
                        // Es el organizador
                        scrim.getOrganizadorId().equals(user.getId()) ||
                                // O está en el mapa de postulados
                                postulacionesMap.containsKey(scrim.getId())
                )
                .map(scrim -> {
                    // Si es el organizador, el estado de postulación es null.
                    // Si no, busca el estado en el mapa.
                    PostulacionState state = scrim.getOrganizadorId().equals(user.getId())
                            ? null
                            : postulacionesMap.get(scrim.getId());
                    return new MyScrimResponse(scrim, state);
                })
                .sorted(Comparator.comparing((MyScrimResponse res) -> res.getScrim().getFechaHora()).reversed())
                .collect(Collectors.toList());
    }
    /**
     * Obtiene todas las postulaciones para un scrim específico.
     * @param scrimId El ID del scrim.
     * @return Lista de postulaciones.
     */
// --- 5. MODIFICAR ESTE MÉTODO ---

    /**
     * Obtiene todas las postulaciones para un scrim específico, CON USERNAME.
     * @param scrimId El ID del scrim.
     * @return Lista de PostulacionResponse (DTO con username).
     */
    public List<PostulacionResponse> getPostulacionesForScrim(UUID scrimId) throws IOException {
        List<Postulacion> allPostulaciones = persistenceManager.readCollection(POSTULACIONES_FILE, Postulacion.class);

        return allPostulaciones.stream()
                .filter(p -> p.getScrimId().equals(scrimId))
                .sorted(Comparator.comparing(Postulacion::getFechaPostulacion))
                .map(postulacion -> {
                    // Por cada postulación, buscamos el User para sacar el username
                    User user = userService.findUserById(postulacion.getUsuarioId()).orElse(null);
                    // Creamos el DTO de respuesta
                    return new PostulacionResponse(postulacion, user);
                })
                .collect(Collectors.toList());
    }

    // --- 6. AÑADIR NUEVOS MÉTODOS PARA ACEPTAR/RECHAZAR ---

    /**
     * Acepta la postulación de un jugador.
     * @param scrimId El ID del scrim
     * @param postulacionId El ID de la postulación
     * @param organizador El usuario (autenticado) que realiza la acción
     */
    public void aceptarPostulacion(UUID scrimId, UUID postulacionId, User organizador) throws IOException {
        Scrim scrim = findScrimById(scrimId);
        if (!scrim.getOrganizadorId().equals(organizador.getId())) {
            throw new UnauthorizedException("Solo el organizador puede aceptar postulaciones.");
        }

        List<Postulacion> all = persistenceManager.readCollection(POSTULACIONES_FILE, Postulacion.class);
        Postulacion post = all.stream()
                .filter(p -> p.getId().equals(postulacionId) && p.getScrimId().equals(scrimId))
                .findFirst()
                .orElseThrow(() -> new ScrimNotFoundException("Postulación no encontrada."));

        post.setEstado(PostulacionState.ACEPTADA);
        post.setHasConfirmed(false);        // <-- importante

        persistenceManager.writeCollection(POSTULACIONES_FILE, all);
        logger.info("[AUDIT] Organizador {} aceptó postulación {}", organizador.getId(), postulacionId);

        // Regla: LOBBY_ARMADO se alcanza cuando hay suficientes ACEPTADAS (sin confirmar).
        this.checkAndProcessLobbyArmado(scrim);
    }

    // --- 1. PEGA ESTE MÉTODO COMPLETO (basado en tu ScrimMatchingSubscriber) ---
    /**
     * Revisa si un scrim llenó su cupo de aceptados y, si es así,
     * cambia su estado a LOBBY_ARMADO y notifica.
     * @param scrim El scrim a revisar.
     */
    private void checkAndProcessLobbyArmado(Scrim scrim) throws IOException {
        if (scrim.getEstado() != ScrimStateEnum.BUSCANDO) return;
        Integer cupo = scrim.getCupo();
        if (cupo == null) return;

        List<Postulacion> all = persistenceManager.readCollection(POSTULACIONES_FILE, Postulacion.class);
        long aceptadas = all.stream()
                .filter(p -> p.getScrimId().equals(scrim.getId()))
                .filter(p -> p.getEstado() == PostulacionState.ACEPTADA)
                .count();

        if (aceptadas >= cupo) {
            scrim.setEstado(ScrimStateEnum.LOBBY_ARMADO);
            List<Scrim> current = persistenceManager.readCollection(SCRIMS_FILE, Scrim.class);
            for (Scrim s : current) {
                if (s.getId().equals(scrim.getId())) { s.setEstado(ScrimStateEnum.LOBBY_ARMADO); break; }
            }
            persistenceManager.writeCollection(SCRIMS_FILE, current);
            eventBus.publish(new LobbyArmadoEvent(scrim));
            logger.info("LobbyArmadoEvent publicado para {}", scrim.getId());
        }
    }


    /**
     * Rechaza la postulación de un jugador.
     * @param scrimId El ID del scrim
     * @param postulacionId El ID de la postulación
     * @param organizador El usuario (autenticado) que realiza la acción
     */
    public void rechazarPostulacion(UUID scrimId, UUID postulacionId, User organizador) throws IOException {
        Scrim scrim = findScrimById(scrimId);
        if (!scrim.getOrganizadorId().equals(organizador.getId())) {
            throw new UnauthorizedException("Solo el organizador puede rechazar postulaciones.");
        }

        List<Postulacion> all = persistenceManager.readCollection(POSTULACIONES_FILE, Postulacion.class);
        Postulacion post = all.stream()
                .filter(p -> p.getId().equals(postulacionId) && p.getScrimId().equals(scrimId))
                .findFirst()
                .orElseThrow(() -> new ScrimNotFoundException("Postulación no encontrada."));

        post.setEstado(PostulacionState.RECHAZADA);
        post.setHasConfirmed(false); // ← opcional pero recomendado

        persistenceManager.writeCollection(POSTULACIONES_FILE, all);
        logger.info("[AUDIT] Organizador {} rechazó postulación {}", organizador.getId(), postulacionId);
    }

    // --- FIN DE NUEVOS MÉTODOS ---


}


