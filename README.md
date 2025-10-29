eScrim: Plataforma de Organización de Scrims (scrim_pds)

Este proyecto es una API REST implementada con Java 17, Spring Boot y Maven, diseñada para facilitar la organización y participación en partidas amistosas (scrims) de eSports. Sigue los requisitos definidos en el TPO de Proceso de Desarrollo de Software (UADE).

Nota: Esta implementación utiliza archivos JSON locales para la persistencia de datos como requerimiento específico del TPO.

Características Principales

Registro y Autenticación de usuarios (simple, basado en token de sesión).

Creación de Scrims especificando juego, formato, región, rangos, etc.

Búsqueda de Scrims con filtros.

Sistema de postulación a Scrims.

Gestión del ciclo de vida del Scrim (Buscando, Lobby Armado, Confirmado, Finalizado, Cancelado - basado en Patrón State).

Carga de estadísticas al finalizar un Scrim.

Persistencia de datos en archivos JSON (data/).

Manejo de concurrencia y backups automáticos (.bak) para los archivos JSON.

Requisitos Previos

Java JDK 17: Asegúrate de tener instalado el JDK 17 o superior. Puedes verificar con java -version.

Apache Maven: Necesitas Maven para compilar y ejecutar el proyecto. Puedes verificar con mvn -version.

Archivos de Datos

Todos los datos de la aplicación (usuarios, scrims, sesiones, etc.) se guardan en archivos JSON dentro de la carpeta data/ en la raíz del proyecto.

data/users.json: Información de los usuarios registrados.

data/sessions.json: Tokens de sesión activos.

data/scrims.json: Detalles de los scrims creados.

data/postulaciones.json: Postulaciones de jugadores a scrims.

data/estadisticas.json: Estadísticas de los scrims finalizados.

Ejecutar Tests

El proyecto incluye tests unitarios y de integración de ejemplo. 
