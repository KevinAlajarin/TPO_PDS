package com.scrim_pds.matchmaking;

import com.scrim_pds.model.Postulacion;
import com.scrim_pds.model.Scrim;
import com.scrim_pds.model.User;
// import com.scrim_pds.service.UserService; // Necesario
// import com.scrim_pds.service.ReportService; // Necesitaríamos un servicio para reportes
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ByHistoryStrategy implements MatchmakingStrategy {

    // private final UserService userService;
    // private final ReportService reportService; // Asumiendo que existe

    // public ByHistoryStrategy(UserService userService, ReportService reportService) {
    //     this.userService = userService;
    //     this.reportService = reportService;
    // }

    @Override
    public List<User> seleccionar(List<Postulacion> candidates, Scrim scrim) {
        System.out.println("[STRATEGY] Executing ByHistoryStrategy (simulated)...");
        // TODO: Implementar lógica real de filtrado basada en strikes, cooldown o reportes.
        // 1. Obtener los User correspondientes a los `candidates`.
        // 2. Filtrar usuarios que tengan `strikes` > X o `cooldownHasta` > now().
        // 3. (Opcional) Consultar `reportService` para ver si tienen reportes graves recientes.
        // 4. Seleccionar entre los candidatos "limpios".
        // 5. Devolver la lista de User.

        // Simulación
        return Collections.emptyList();
    }
}

