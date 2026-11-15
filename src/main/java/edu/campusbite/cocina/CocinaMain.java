package edu.campusbite.cocina;

import edu.campusbite.common.rmi.ConfiguracionRmi;
import edu.campusbite.common.rmi.ServicioCocina;
import edu.campusbite.common.util.Registro;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/** Inicia Cocina en su propia JVM */
public class CocinaMain {

    private static Registry registro;
    private static ServicioCocinaImpl servicio;
    private static ApiCocina api;

    public static void main(String[] args) throws Exception {
        ConfiguracionRmi.configurarHostServidor();
        ConfiguracionRmi.configurarTimeoutsCliente(2000, 3000);

        Runtime.getRuntime().addShutdownHook(
                new Thread(CocinaMain::detener, "cierre-cocina"));

        try {
            servicio = new ServicioCocinaImpl();

            ServicioCocina stub =
                    (ServicioCocina) UnicastRemoteObject.exportObject(servicio, 0);

            registro = LocateRegistry.createRegistry(
                    ConfiguracionRmi.PUERTO_COCINA);
            registro.rebind(ServicioCocina.NOMBRE, stub);

            api = new ApiCocina(servicio, ConfiguracionRmi.HTTP_COCINA);
            api.iniciar();

            Registro.info("COCINA",
                    "RMI '" + ServicioCocina.NOMBRE + "' en puerto "
                            + ConfiguracionRmi.PUERTO_COCINA
                            + " | API HTTP en http://localhost:"
                            + ConfiguracionRmi.HTTP_COCINA);

        } catch (Exception e) {
            detener();
            throw e;
        }
    }

    private static void detener() {
        if (api != null) {
            api.detener();
            api = null;
        }

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
    }
}