package edu.campusbite.notificaciones;

import edu.campusbite.common.rmi.ConfiguracionRmi;
import edu.campusbite.common.rmi.ServicioNotificaciones;
import edu.campusbite.common.util.Registro;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.rmi.NoSuchObjectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

public class NotificacionesMain {

    private static final int PUERTO_HTTP = 8081;

    private static ConfigurableApplicationContext contexto;
    private static Registry registro;
    private static ServicioNotificacionesImpl servicio;

    public static void main(String[] args) throws Exception {
        ConfiguracionRmi.configurarHostServidor();
        ConfiguracionRmi.configurarTimeoutsCliente(2000, 3000);

        Runtime.getRuntime().addShutdownHook(
                new Thread(NotificacionesMain::detener,
                        "cierre-notificaciones"));

        try {
            SpringApplication aplicacion =
                    new SpringApplication(NotificacionesApplication.class);

            aplicacion.setDefaultProperties(
                    Map.<String, Object>of("server.port", PUERTO_HTTP));

            contexto = aplicacion.run(args);

            NotificacionesWebSocketHandler manejador =
                    contexto.getBean(NotificacionesWebSocketHandler.class);
            servicio = new ServicioNotificacionesImpl(manejador);

            ServicioNotificaciones stub =
                    (ServicioNotificaciones)
                            UnicastRemoteObject.exportObject(servicio, 0);

            registro = LocateRegistry.createRegistry(
                    ConfiguracionRmi.PUERTO_NOTIFICACIONES);
            registro.rebind(ServicioNotificaciones.NOMBRE, stub);

            Registro.info("NOTIFICACIONES",
                    "RMI '" + ServicioNotificaciones.NOMBRE
                            + "' en puerto "
                            + ConfiguracionRmi.PUERTO_NOTIFICACIONES
                            + " | WebSocket en ws://localhost:"
                            + PUERTO_HTTP + "/ws?pedidoId={id}");

        } catch (Exception e) {
            detener();
            throw e;
        }
    }

    private static void detener() {
        if (servicio != null) {
            try {
                UnicastRemoteObject.unexportObject(servicio, true);
            } catch (NoSuchObjectException ignorada) {
                // El servicio ya estaba detenido.
            }
            servicio = null;
        }

        if (registro != null) {
            try {
                UnicastRemoteObject.unexportObject(registro, true);
            } catch (NoSuchObjectException ignorada) {
                // El registro ya estaba detenido.
            }
            registro = null;
        }

        if (contexto != null) {
            contexto.close();
            contexto = null;
        }
    }
}