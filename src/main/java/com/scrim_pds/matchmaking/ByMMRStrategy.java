package com.scrim_pds.matchmaking;

import com.scrim_pds.model.Postulacion;
import com.scrim_pds.model.Scrim;
import com.scrim_pds.model.User;
// Necesitaríamos inyectar UserService para buscar los usuarios
// import com.scrim_pds.service.UserService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component // Marcar como Bean de Spring
public class ByMMRStrategy implements MatchmakingStrategy {

    // private final UserService userService; // Necesitaría inyectarse

    // public ByMMRStrategy(UserService userService) { // Constructor
    //     this.userService = userService;
    // }

    @Override
    public List<User> seleccionar(List<Postulacion> candidates, Scrim scrim) {
        System.out.println("[STRATEGY] Executing ByMMRStrategy (simulated)...");
        // TODO: Implementar lógica real de filtrado y selección por MMR/Rango
        // 1. Obtener los IDs de usuario de los candidatos.
        // 2. Usar userService.findUserById para buscar cada User.
        // 3. Filtrar usuarios cuyo rango (en el juego del scrim) esté entre scrim.getRangoMin() y scrim.getRangoMax().
        //    (Requiere una función compleja para comparar rangos).
        // 4. Ordenar/seleccionar según alguna lógica (ej. los más cercanos al rango medio).
        // 5. Devolver la lista de User seleccionados.

        // Simulación: Devolvemos una lista vacía por ahora
        return Collections.emptyList();
    }
}

