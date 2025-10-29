package com.scrim_pds.matchmaking;

import com.scrim_pds.model.Scrim;
import com.scrim_pds.model.User;
import com.scrim_pds.model.Postulacion; // Podría necesitar postulaciones

import java.util.List;

/**
 * Interfaz para el Patrón Strategy de Matchmaking (RF6).
 */
public interface MatchmakingStrategy {

    /**
     * Selecciona a los jugadores que participarán en el scrim.
     * @param candidatos Lista de Postulacion que se postularon.
     * @param scrim El scrim para el cual se está seleccionando.
     * @return Lista de User seleccionados para formar los equipos (simplificado, podría devolver los IDs o las postulaciones).
     */
    List<User> seleccionar(List<Postulacion> candidatos, Scrim scrim);
}

