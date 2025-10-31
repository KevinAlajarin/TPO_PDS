package com.scrim_pds.service;

import com.scrim_pds.dto.FeedbackCreateRequest;
import com.scrim_pds.dto.ModerationRequest;
import com.scrim_pds.exception.FeedbackNotAllowedException;
// --- IMPORT CORREGIDO ---
import com.scrim_pds.exception.ScrimNotFoundException; // Usar esta en lugar de ResourceNotFoundException
import com.scrim_pds.model.Feedback;
import com.scrim_pds.model.Scrim;
import com.scrim_pds.model.User;
import com.scrim_pds.model.enums.ModerationState;
import com.scrim_pds.model.enums.ScrimStateEnum;
import com.scrim_pds.persistence.JsonPersistenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList; // Asegurarse que esté importado
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FeedbackService {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackService.class);
    private final JsonPersistenceManager persistenceManager;
    private final ScrimService scrimService; // Para buscar participantes
    private final String FEEDBACK_FILE = "feedback.json";

    public FeedbackService(JsonPersistenceManager persistenceManager, ScrimService scrimService) {
        this.persistenceManager = persistenceManager;
        this.scrimService = scrimService;
    }

    /**
     * Permite a un participante dejar feedback sobre otro jugador en un scrim finalizado.
     */
    public Feedback createFeedback(UUID scrimId, User reviewer, FeedbackCreateRequest dto) throws IOException {
        // 1. Validar el Scrim
        Scrim scrim = scrimService.findScrimById(scrimId); // Reutiliza el método (lanza ScrimNotFound)
        if (scrim.getEstado() != ScrimStateEnum.FINALIZADO) {
            throw new FeedbackNotAllowedException("Solo se puede dejar feedback en scrims finalizados.");
        }

        // 2. Validar que el Reviewer y el Target sean participantes
        List<User> participants = scrimService.findParticipantsForScrim(scrimId, scrim.getOrganizadorId());
        
        boolean reviewerIsParticipant = participants.stream().anyMatch(p -> p.getId().equals(reviewer.getId()));
        if (!reviewerIsParticipant) {
            throw new FeedbackNotAllowedException("No puedes dejar feedback porque no participaste en este scrim.");
        }
        
        boolean targetIsParticipant = participants.stream().anyMatch(p -> p.getId().equals(dto.getTargetUserId()));
        if (!targetIsParticipant) {
            throw new FeedbackNotAllowedException("No se puede dejar feedback a un usuario que no participó en este scrim.");
        }

        // 3. Validar que no se esté auto-evaluando
        if (reviewer.getId().equals(dto.getTargetUserId())) {
            throw new FeedbackNotAllowedException("No puedes dejarte feedback a ti mismo.");
        }

        // 4. Validar que no haya enviado ya feedback a este usuario en este scrim
        List<Feedback> allFeedback = persistenceManager.readCollection(FEEDBACK_FILE, Feedback.class);
        boolean alreadySubmitted = allFeedback.stream().anyMatch(f ->
                f.getScrimId().equals(scrimId) &&
                f.getReviewerId().equals(reviewer.getId()) &&
                f.getTargetUserId().equals(dto.getTargetUserId())
        );
        if (alreadySubmitted) {
            throw new FeedbackNotAllowedException("Ya has enviado feedback para este jugador en este scrim.");
        }

        // 5. Todo válido. Crear y guardar.
        Feedback newFeedback = new Feedback();
        newFeedback.setId(UUID.randomUUID());
        newFeedback.setScrimId(scrimId);
        newFeedback.setReviewerId(reviewer.getId());
        newFeedback.setTargetUserId(dto.getTargetUserId());
        newFeedback.setRating(dto.getRating());
        newFeedback.setComment(dto.getComment());
        newFeedback.setModerationState(ModerationState.PENDIENTE); // Siempre PENDIENTE al crear
        newFeedback.setCreatedAt(LocalDateTime.now());

        allFeedback.add(newFeedback);
        persistenceManager.writeCollection(FEEDBACK_FILE, allFeedback);
        
        logger.info("[AUDIT] Usuario '{}' (ID: {}) envió feedback para Usuario '{}' en Scrim {}", 
                reviewer.getUsername(), reviewer.getId(), dto.getTargetUserId(), scrimId);

        return newFeedback;
    }

    /**
     * Obtiene todo el feedback APROBADO para un scrim específico.
     */
    public List<Feedback> getApprovedFeedbackForScrim(UUID scrimId) throws IOException {
        List<Feedback> allFeedback = persistenceManager.readCollection(FEEDBACK_FILE, Feedback.class);
        
        return allFeedback.stream()
                .filter(f -> f.getScrimId().equals(scrimId) && 
                             f.getModerationState() == ModerationState.APROBADO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene todo el feedback PENDIENTE (para moderadores).
     */
    public List<Feedback> getPendingFeedback() throws IOException {
        List<Feedback> allFeedback = persistenceManager.readCollection(FEEDBACK_FILE, Feedback.class);
        return allFeedback.stream()
                .filter(f -> f.getModerationState() == ModerationState.PENDIENTE)
                .collect(Collectors.toList());
    }


    /**
     * Modera un feedback (aprueba o rechaza).
     */
    public Feedback moderateFeedback(UUID feedbackId, ModerationRequest dto) throws IOException {
        List<Feedback> allFeedback = persistenceManager.readCollection(FEEDBACK_FILE, Feedback.class);
        
        Optional<Feedback> feedbackOpt = allFeedback.stream().filter(f -> f.getId().equals(feedbackId)).findFirst();
        if (feedbackOpt.isEmpty()) {
            // --- EXCEPCIÓN CORREGIDA ---
            throw new ScrimNotFoundException("No se encontró el feedback con ID: " + feedbackId);
        }

        Feedback feedback = feedbackOpt.get();
        
        if (dto.getNewState() == ModerationState.PENDIENTE) {
             throw new FeedbackNotAllowedException("No se puede moderar a estado PENDIENTE.");
        }
        
        feedback.setModerationState(dto.getNewState());
        
        persistenceManager.writeCollection(FEEDBACK_FILE, allFeedback);
        logger.info("[AUDIT] Feedback {} moderado a estado {}", feedbackId, dto.getNewState());

        return feedback;
    }
}

