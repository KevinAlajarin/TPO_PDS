package com.scrim_pds.matchmaking;

import com.scrim_pds.model.Postulacion;
import com.scrim_pds.model.Scrim;
import com.scrim_pds.model.User;
// import com.scrim_pds.service.UserService; // Podría necesitarse
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ByLatencyStrategy implements MatchmakingStrategy {

    // private final UserService userService; // Podría necesitarse para devolver User

    // public ByLatencyStrategy(UserService userService) {
    //     this.userService = userService;
    // }

    @Override
    public List<User> seleccionar(List<Postulacion> candidates, Scrim scrim) {
        System.out.println("[STRATEGY] Executing ByLatencyStrategy (simulated)...");
        // TODO: Implementar lógica real de filtrado por latenciaReportada vs scrim.getLatenciaMax
        // 1. Filtrar la lista `candidates` manteniendo solo aquellos donde p.getLatenciaReportada() <= scrim.getLatenciaMax().
        // 2. Ordenar por latencia (opcional).
        // 3. Seleccionar los necesarios para llenar el cupo.
        // 4. Obtener los objetos User correspondientes (requiere UserService).
        // 5. Devolver la lista de User.

        // Simulación
        return Collections.emptyList();
    }
}

