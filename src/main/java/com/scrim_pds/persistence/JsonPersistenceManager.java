package com.scrim_pds.persistence;

import com.fasterxml.jackson.databind.ObjectMapper; // No necesitas TypeReference aquí
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Gestiona la persistencia de colecciones de objetos en archivos JSON.
 * Proporciona concurrencia (locking por archivo) y escrituras atómicas.
 */
@Component
public class JsonPersistenceManager {

    private static final Logger logger = LoggerFactory.getLogger(JsonPersistenceManager.class);

    private final ObjectMapper objectMapper;
    private final Path dataDirectory;

    // Un mapa de locks, uno para cada archivo JSON, para manejar concurrencia
    private final Map<String, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();

    // --- NUEVO ---
    // Lista de todos los archivos JSON que usará la aplicación
    private static final List<String> REQUIRED_FILES = List.of(
            "users.json",
            "sessions.json",
            "scrims.json",
            "postulaciones.json",
            "estadisticas.json",
            "verifications.json"
            // Añade aquí futuros archivos si los necesitas
    );

    public JsonPersistenceManager(ObjectMapper objectMapper, @Value("${data.directory}") String dataDirPath) {
        this.objectMapper = objectMapper;
        this.dataDirectory = Paths.get(dataDirPath);
    }

    // --- MÉTODO INIT MODIFICADO ---
    /**
     * Se asegura de que el directorio 'data/' y TODOS los archivos JSON requeridos existan al iniciar.
     */
    @PostConstruct
    public void init() {
        try {
            // 1. Crear directorio si no existe
            if (Files.notExists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
                logger.info("Directorio de datos creado en: {}", dataDirectory.toAbsolutePath());
            } else {
                logger.info("Usando directorio de datos existente: {}", dataDirectory.toAbsolutePath());
            }

            // 2. Crear cada archivo JSON si no existe (con lista vacía [])
            for (String fileName : REQUIRED_FILES) {
                Path filePath = getFilePath(fileName);
                if (Files.notExists(filePath)) {
                    logger.warn("Archivo {} no encontrado. Creando archivo vacío.", fileName);
                    // Usamos writeCollection para crearlo de forma segura (con lock)
                    // No hay riesgo de deadlock aquí porque @PostConstruct se ejecuta una sola vez al inicio.
                    writeCollection(fileName, new ArrayList<>());
                    logger.info("Archivo {} creado exitosamente.", fileName);
                }
            }

        } catch (IOException e) {
            logger.error("Error fatal durante la inicialización del directorio/archivos de datos: {}", dataDirectory.toAbsolutePath(), e);
            // Considera detener la aplicación si la inicialización falla
            // System.exit(1);
        } catch (IllegalArgumentException e) {
             logger.error("Error fatal: Nombre de archivo inválido en REQUIRED_FILES: {}", e.getMessage());
             // System.exit(1);
        }
    }

    /**
     * Obtiene el lock para un nombre de archivo, creándolo si no existe.
     */
    private ReentrantReadWriteLock getLock(String fileName) {
        return locks.computeIfAbsent(fileName, k -> new ReentrantReadWriteLock());
    }

    /**
     * Resuelve la ruta completa de un archivo en el directorio 'data/'.
     */
    private Path getFilePath(String fileName) {
        // Sanitización simple para evitar Path Traversal
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new IllegalArgumentException("Nombre de archivo inválido: " + fileName);
        }
        return dataDirectory.resolve(fileName);
    }

    // --- MÉTODO readCollection MODIFICADO ---
    /**
     * Lee una colección de objetos desde un archivo JSON.
     * Es seguro para concurrencia (usa ReadLock).
     *
     * @param fileName Nombre del archivo (ej. "users.json")
     * @param itemClass El tipo de la clase en la lista (ej. User.class)
     * @return Una lista de objetos, o una lista vacía si el archivo está vacío o corrupto.
     */
    public <T> List<T> readCollection(String fileName, Class<T> itemClass) throws IOException {
        ReentrantReadWriteLock.ReadLock readLock = getLock(fileName).readLock();
        readLock.lock();
    
        Path filePath = getFilePath(fileName);
    
        try {
            // AHORA ESTAMOS SEGUROS DE QUE EL ARCHIVO EXISTE gracias a init()
            // Ya no necesitamos el bloque 'if (Files.notExists...)' que causaba el deadlock
    
            // Si el archivo está vacío (ej. justo después de crearlo), devuelve lista vacía
            if (Files.size(filePath) == 0) {
                 logger.debug("Archivo {} está vacío, devolviendo lista vacía.", fileName);
                 return new ArrayList<>();
            }

            try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                // Construye el tipo List<T> para Jackson
                com.fasterxml.jackson.databind.type.CollectionType javaType = objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, itemClass);
                
                // Añadir manejo por si el JSON está malformado o no es una lista
                try {
                     return objectMapper.readValue(reader, javaType);
                } catch (com.fasterxml.jackson.databind.JsonMappingException | com.fasterxml.jackson.core.JsonParseException e) { // Capturar ambos
                     logger.error("Error al parsear JSON en {}. ¿Contenido es una lista JSON válida? {}", fileName, e.getMessage());
                     // Devuelve lista vacía o lanza una excepción específica si prefieres
                     return new ArrayList<>();
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Escribe (sobrescribe) una colección completa a un archivo JSON.
     * Es atómico (usa write-to-temp-then-rename) y seguro para concurrencia (usa WriteLock).
     * También crea un backup (.bak) del archivo anterior.
     *
     * @param fileName Nombre del archivo (ej. "users.json")
     * @param collection La colección de objetos a guardar.
     */
    public void writeCollection(String fileName, Collection<?> collection) throws IOException {
        ReentrantReadWriteLock.WriteLock writeLock = getLock(fileName).writeLock();
        writeLock.lock();
        
        Path filePath = getFilePath(fileName);
        Path tempPath = getFilePath(fileName + ".tmp");
        Path bakPath = getFilePath(fileName + ".bak");

        try {
            // 1. Escribir al archivo temporal
            try (BufferedWriter writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8)) {
                objectMapper.writeValue(writer, collection);
            }

            // 2. Crear backup del archivo actual (si existe)
            if (Files.exists(filePath)) {
                Files.move(filePath, bakPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // 3. Renombrar el temporal al archivo final (operación atómica)
            Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            logger.error("Error al escribir en {}: {}", fileName, e.getMessage());
            // Intentar restaurar desde el temporal o backup si algo falló
            try { // Usar try-with-resources aquí también por si acaso
                if (Files.exists(tempPath)) {
                    Files.deleteIfExists(tempPath);
                }
                if (Files.exists(bakPath) && !Files.exists(filePath)) {
                    logger.warn("Restaurando backup {} a {}", bakPath, filePath);
                    Files.move(bakPath, filePath); // Restaurar backup
                }
            } catch (IOException ex) {
                logger.error("Error adicional intentando limpiar/restaurar después de fallo de escritura: {}", ex.getMessage());
            }
            throw e; // Relanzar la excepción original
        } finally {
            writeLock.unlock();
        }
    }
}
