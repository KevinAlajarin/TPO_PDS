package com.scrim_pds.dto;

import com.scrim_pds.model.Scrim;
import com.scrim_pds.model.enums.PostulacionState; // <-- Asegúrate de importar tu Enum

public class MyScrimResponse {
    private Scrim scrim;
    private PostulacionState postulationState; // null si eres el organizador

    public MyScrimResponse(Scrim scrim, PostulacionState state) {
        this.scrim = scrim;
        this.postulationState = state;
    }

    // Getters (necesarios para que Jackson los convierta a JSON)
    public Scrim getScrim() { return scrim; }
    public PostulacionState getPostulationState() { return postulationState; }
}