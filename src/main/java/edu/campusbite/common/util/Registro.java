package edu.campusbite.common.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

// Log simple con hora, servicio e hilo (sirve como evidencia en las pruebas)
public final class Registro {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Registro() {
    }

    // Metodo escrito con ayuda de IA (Claude): unifica el log que antes estaba duplicado en Pagos y Administracion.
    public static void info(String servicio, String mensaje) {
        System.out.println("[" + LocalTime.now().format(HORA) + "] [" + servicio + "] ["
                + Thread.currentThread().getName() + "] " + mensaje);
    }
}