package edu.campusbite.pedidos;

import edu.campusbite.common.rmi.ConfiguracionRmi;
import edu.campusbite.common.rmi.OperacionRechazadaException;
import edu.campusbite.common.rmi.ServicioEstadoPedidos;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

// --------------------- DAVID ---------


// ESTO  PUBLICA EL METODO REMOTO PARA RECIBIR LISTO. El que lo ejecuta es INICIAR() empezando a llamar a
// .configurarHostServidor para carga incial, y usando un try and catch para facilitar la captura de
// del error y su depuración.

@Component
@ConditionalOnProperty(name = "campusbite.cocina.habilitada", havingValue = "true")
public class PedidosRmi implements ServicioEstadoPedidos {
    private final ServicioPedidos pedidos;
    private final int puerto;
    private Registry registro;

    public PedidosRmi(ServicioPedidos pedidos,
                      @Value("${campusbite.rmi.estado-pedidos:1105}") int puerto) {
        this.pedidos = pedidos;
        this.puerto = puerto;
    }

    @PostConstruct
    public void iniciar() throws RemoteException {
        ConfiguracionRmi.configurarHostServidor();
        try {
            registro = LocateRegistry.createRegistry(puerto);
            ServicioEstadoPedidos stub = (ServicioEstadoPedidos) UnicastRemoteObject.exportObject(this, 0);
            registro.rebind(ServicioEstadoPedidos.NOMBRE, stub);
        } catch (RemoteException e) {
            cerrar();
            throw e;
        }
    }

    @Override
    public boolean marcarListo(String pedidoId) throws OperacionRechazadaException {
        return pedidos.marcarListo(pedidoId);
    }
// Generado con IA (CLAUDE): Genero el metodo completo porque no sabia como eliminar o terminar el servicio RMI  de pedidos
    @PreDestroy
    public void cerrar() {
        try { UnicastRemoteObject.unexportObject(this, true); }
        catch (NoSuchObjectException ignorada) { }
        if (registro != null) {
            try { UnicastRemoteObject.unexportObject(registro, true); }
            catch (NoSuchObjectException ignorada) { }
        }
    }
}
