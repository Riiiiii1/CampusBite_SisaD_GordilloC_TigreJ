package edu.campusbite.common.rmi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMISocketFactory;

/**
 * Puertos y opciones RMI acordados por el grupo.
 * Cada servicio crea su propio registro en su JVM.
 */
public final class ConfiguracionRmi {

    public static final String HOST = "localhost";

    // Registros RMI (uno por servicio, cada uno en su JVM)
    public static final int PUERTO_PAGOS = 1101;
    public static final int PUERTO_COCINA = 1102;
    public static final int PUERTO_ADMINISTRACION = 1103;
    public static final int PUERTO_NOTIFICACIONES = 1104;

    // ------------------------------- MODIFICADO DAVID - ORIGINAL JONH -------------------------------
    public static final int PUERTO_ESTADO_PEDIDOS = 1105;
    public static final int HTTP_COCINA = 8084;

    // HTTP: Pedidos (REST) 8080, WebSocket de Notificaciones 8081
    public static final int HTTP_PAGOS = 8082;
    public static final int HTTP_ADMINISTRACION = 8083;

    private ConfiguracionRmi() {
    }

    // ------------------------------- MODIFICADO DAVID - ORIGINAL JONH -------------------------------
    public static synchronized void configurarTimeoutsCliente(long connectMs, long responseMs) {
        if (connectMs <= 0 || responseMs <= 0 || connectMs > Integer.MAX_VALUE || responseMs > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Los limites RMI deben ser positivos y caber en un int");
        }
        RMISocketFactory actual = RMISocketFactory.getSocketFactory();
        if (actual == null) {
            try {
                RMISocketFactory.setSocketFactory(new SocketsConLimite((int) connectMs, (int) responseMs));
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo configurar el transporte RMI", e);
            }
        } else if (!(actual instanceof SocketsConLimite limites)
                || limites.conexionMs != connectMs || limites.respuestaMs != responseMs) {
            throw new IllegalStateException("Esta JVM ya tiene otra configuracion de sockets RMI");
        }
        System.setProperty("sun.rmi.transport.tcp.handshakeTimeout", String.valueOf(responseMs));
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", String.valueOf(responseMs));
    }

    // ------------------------------- MODIFICADO DAVID - ORIGINAL JONH -------------------------------
    private static final class SocketsConLimite extends RMISocketFactory {
        private final int conexionMs;
        private final int respuestaMs;

        private SocketsConLimite(int conexionMs, int respuestaMs) {
            this.conexionMs = conexionMs;
            this.respuestaMs = respuestaMs;
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(host, port), conexionMs);
                socket.setSoTimeout(respuestaMs);
                return socket;
            } catch (IOException e) {
                socket.close();
                throw e;
            }
        }

        @Override
        public ServerSocket createServerSocket(int port) throws IOException { return new ServerSocket(port); }
    }

    /** Evita que RMI anuncie una IP de red en Windows. Llamar en cada servidor. */
    public static void configurarHostServidor() {
        if (System.getProperty("java.rmi.server.hostname") == null) {
            System.setProperty("java.rmi.server.hostname", HOST);
        }
    }
}
