package edu.campusbite.common.rmi;

/**
 * Puertos y opciones RMI acordados por el grupo.
 * Cada servicio crea su propio registro en su JVM.
 */
public final class ConfiguracionRmi {

    public static final String HOST = "localhost";

    public static final int PUERTO_PAGOS = 1099;
    public static final int PUERTO_COCINA = 1100;
    public static final int PUERTO_NOTIFICACIONES = 1101;
    public static final int PUERTO_ADMINISTRACION = 1102;

    private ConfiguracionRmi() {
    }

    /**
     * Configura los limites de espera de los clientes RMI de esta JVM.
     * Debe llamarse al inicio del main, ANTES del primer lookup.
     *
     * - connectTimeout: cuanto espera para abrir la conexion TCP.
     * - responseTimeout: cuanto espera la respuesta una vez enviada la llamada.
     *   Es un timeout del socket: si se cumple, el hilo del cliente se libera
     *   con una RemoteException (no queda bloqueado como con Future.get).
     *   OJO: el servidor puede seguir ejecutando la operacion y terminarla
     *   despues; por eso las operaciones deben ser idempotentes.
     */
    public static void configurarTimeoutsCliente(long connectMs, long responseMs) {
        System.setProperty("sun.rmi.transport.proxy.connectTimeout", String.valueOf(connectMs));
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", String.valueOf(responseMs));
    }

    /** Evita que RMI anuncie una IP de red en Windows. Llamar en cada servidor. */
    public static void configurarHostServidor() {
        if (System.getProperty("java.rmi.server.hostname") == null) {
            System.setProperty("java.rmi.server.hostname", HOST);
        }
    }
}
