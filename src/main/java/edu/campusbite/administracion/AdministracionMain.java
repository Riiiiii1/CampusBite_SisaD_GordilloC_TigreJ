package edu.campusbite.administracion;

import edu.campusbite.common.rmi.ConfiguracionRmi;
import edu.campusbite.common.rmi.ServicioAdministracion;
import edu.campusbite.common.util.Registro;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class AdministracionMain {

    // Referencias estaticas: evitan que el recolector de basura elimine el registro o el servicio.
    private static Registry registro;
    private static ServicioAdministracionImpl servicio;

    public static void main(String[] args) throws Exception {
        ConfiguracionRmi.configurarHostServidor();

        servicio = new ServicioAdministracionImpl();
        ServicioAdministracion stub = (ServicioAdministracion) UnicastRemoteObject.exportObject(servicio, 0);
        registro = LocateRegistry.createRegistry(ConfiguracionRmi.PUERTO_ADMINISTRACION);
        registro.rebind(ServicioAdministracion.NOMBRE, stub);

        new ApiAdministracion(servicio, ConfiguracionRmi.HTTP_ADMINISTRACION).iniciar();

        Registro.info("ADMIN", "RMI '" + ServicioAdministracion.NOMBRE + "' en puerto "
                + ConfiguracionRmi.PUERTO_ADMINISTRACION + " | REST en http://localhost:" + ConfiguracionRmi.HTTP_ADMINISTRACION);
    }
}