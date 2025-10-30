package com.scrim_pds.service;

import com.scrim_pds.model.Scrim;
import com.scrim_pds.model.User;
import com.scrim_pds.model.enums.CanalNotificacion;
import com.scrim_pds.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ScheduledTasksService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasksService.class);

    private final ScrimService scrimService;
    private final NotificationService notificationService;

    private static final int REMINDER_HOURS_BEFORE = 2; // Enviar recordatorio 2 horas antes

    public ScheduledTasksService(ScrimService scrimService, NotificationService notificationService) {
        this.scrimService = scrimService;
        this.notificationService = notificationService;
    }

    /**
     * Tarea programada principal. Se ejecuta cada 1 minuto.
     * (fixedRate = 60000 ms = 1 minuto)
     */

    @Scheduled(fixedRate = 60000)
    public void runScheduledTasks() {
        logger.info("--- Ejecutando Tareas Programadas ---");

        // 1. Tarea de Auto-Inicio de Scrims
        try {
            autoStartScrims();
        } catch (Exception e) {
            logger.error("[Scheduler] Error durante la tarea de auto-inicio de scrims:", e);
        }

        // 2. Tarea de envio de Recordatorios
        try {
            sendReminders();
        } catch (Exception e) {
            logger.error("[Scheduler] Error durante la tarea de envío de recordatorios:", e);
        }
        
        logger.info("--- Tareas Programadas Finalizadas ---");
    }

    /**
     * (Auto-Inicio): Busca scrims CONFIRMADO cuya fechaHora sea pasada
     * y llama a ScrimService para iniciarlos.
     */
    private void autoStartScrims() {
        try {
            List<Scrim> scrimsToStart = scrimService.findScrimsToAutoStart();
            if (scrimsToStart.isEmpty()) {
                logger.debug("[Scheduler] No hay scrims para auto-iniciar.");
                return;
            }

            logger.info("[Scheduler] Encontrados {} scrim(s) para auto-iniciar.", scrimsToStart.size());
            for (Scrim scrim : scrimsToStart) {
                try {
                    scrimService.iniciarScrim(scrim.getId(), null);
                } catch (Exception e) {
                    logger.error("[Scheduler] Error al auto-iniciar scrim {}: {}", scrim.getId(), e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error("[Scheduler] Error al leer scrims.json para auto-inicio: {}", e.getMessage());
        }
    }

    /**
     * (Recordatorios): Busca scrims CONFIRMADO que empiecen pronto
     * y notifica a los participantes.
     */

    private void sendReminders() {
        try {
            // 1. Encontrar scrims elegibles 
            List<Scrim> scrimsToRemind = scrimService.findScrimsForReminder(REMINDER_HOURS_BEFORE);
            
            if (scrimsToRemind.isEmpty()) {
                logger.debug("[Scheduler] No hay recordatorios de scrim para enviar.");
                return;
            }

            logger.info("[Scheduler] Encontrados {} scrim(s) para enviar recordatorios.", scrimsToRemind.size());
            
            for (Scrim scrim : scrimsToRemind) {
                boolean allNotifiedSuccessfully = true; 
                try {
                    // 2. Buscar participantes
                    List<User> participants = scrimService.findParticipantsForScrim(scrim.getId(), scrim.getOrganizadorId());
                    logger.debug("Enviando recordatorio del Scrim {} a {} participantes.", scrim.getId(), participants.size());

                    // 3. Enviar notificaciones
                    if (participants.isEmpty()) {
                         logger.warn("[Scheduler] Scrim {} listo para recordatorio, pero no se encontraron participantes (ni organizador).", scrim.getId());
                         allNotifiedSuccessfully = true;
                    }
                    
                    for (User user : participants) {
                        if (shouldNotify(user, CanalNotificacion.EMAIL)) {
                            notificationService.sendScrimReminderNotification(user, scrim);
                        } else {
                            logger.debug("[Scheduler] Usuario {} no desea recordatorios por EMAIL.", user.getId());
                        }
                    }
                    
                } catch (Exception e) {
                    allNotifiedSuccessfully = false; 
                    logger.error("[Scheduler] Error al procesar recordatorios para scrim {}: {}", scrim.getId(), e.getMessage());
                }

                // 4. Marcar como enviado (SOLO si todos los pasos anteriores fueron exitosos)
                if (allNotifiedSuccessfully) {
                    try {
                        scrimService.marcarRecordatorioComoEnviado(scrim.getId());
                    } catch (IOException e) {
                         logger.error("[Scheduler] Error CRÍTICO al marcar recordatorio como enviado para Scrim {}: {}", scrim.getId(), e.getMessage());
                    }
                }
            } 
        } catch (IOException e) {
            logger.error("[Scheduler] Error al leer archivos para enviar recordatorios: {}", e.getMessage());
        }
    }

    private boolean shouldNotify(User user, CanalNotificacion canal) {
        if (user.getPreferencias() == null) {
            logger.debug("[Scheduler] Usuario {} no tiene objeto de preferencias, omitiendo notificación.", user.getId());
            return false;
        }
        boolean wantsReminders = user.getPreferencias().isRecordatoriosActivos();
        boolean hasChannel = user.getPreferencias().getCanalesNotificacion() != null &&
                             user.getPreferencias().getCanalesNotificacion().contains(canal.name());
                             

        return wantsReminders && hasChannel;
    }
}

