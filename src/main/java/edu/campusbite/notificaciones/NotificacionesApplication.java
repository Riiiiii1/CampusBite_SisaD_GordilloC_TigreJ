package edu.campusbite.notificaciones;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Contexto Spring separado para que Notificaciones tenga su propia JVM
 * y no inicie también la aplicación Pedidos
 */
@SpringBootApplication(scanBasePackages = "edu.campusbite.notificaciones")
public class NotificacionesApplication {
}