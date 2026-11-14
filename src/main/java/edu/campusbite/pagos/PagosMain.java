package edu.campusbite.pagos;

import edu.campusbite.common.rmi.ConfiguracionRmi;
import edu.campusbite.common.rmi.ServicioPagos;
import edu.campusbite.common.util.Registro;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

// Arranca el Servicio de Pagos y Facturacion en su propia JVM
public class PagosMain {

    // Referencias estaticas: si el registro o el objeto quedan sin referencia,
    // el recolector de basura puede eliminarlos y el servicio deja de responder.
    private static Registry registro;
    private static ServicioPagosImpl servicio;

    public static void main(String[] args) throws Exception {
        ConfiguracionRmi.configurarHostServidor();

        servicio = new ServicioPagosImpl();

        // Exporta el objeto: RMI crea el stub que viajara a los clientes.
        ServicioPagos stub = (ServicioPagos) UnicastRemoteObject.exportObject(servicio, 0);
        registro = LocateRegistry.createRegistry(ConfiguracionRmi.PUERTO_PAGOS);
        registro.rebind(ServicioPagos.NOMBRE, stub);

        new ApiConsultasPagos(servicio, ConfiguracionRmi.HTTP_PAGOS).iniciar();

        Registro.info("PAGOS", "RMI '" + ServicioPagos.NOMBRE + "' en puerto " + ConfiguracionRmi.PUERTO_PAGOS
                + " | REST de consultas en http://localhost:" + ConfiguracionRmi.HTTP_PAGOS);
    }
}