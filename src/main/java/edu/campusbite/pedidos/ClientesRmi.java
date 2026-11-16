package edu.campusbite.pedidos;

import edu.campusbite.common.dto.*;
import edu.campusbite.common.rmi.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

// --------------------- DAVID ---------
@Component
public class ClientesRmi {
    private final String host;
    private final int puertoPagos;
    private final int puertoAdministracion;
    private final int puertoCocina;


    // GENERADO CON IA (CLAUDE) : Autocompletar usando campusbite.rmi.host → localhost, campusbite.rmi.pagos → 1101
    // .etc y añadiendo la etiqueta VALUE
    public ClientesRmi(@Value("${campusbite.rmi.host:localhost}") String host,
                       @Value("${campusbite.rmi.pagos:1101}") int puertoPagos,
                       @Value("${campusbite.rmi.administracion:1103}") int puertoAdministracion,
                       @Value("${campusbite.rmi.cocina:1102}") int puertoCocina,
                       @Value("${campusbite.rmi.conexion-ms:2000}") int conexionMs,
                       @Value("${campusbite.rmi.respuesta-ms:3000}") int respuestaMs) {
        this.host = host;
        this.puertoPagos = puertoPagos;
        this.puertoAdministracion = puertoAdministracion;
        this.puertoCocina = puertoCocina;
        ConfiguracionRmi.configurarTimeoutsCliente(conexionMs, respuestaMs);
    }

    public ComprobantePago pagar(SolicitudPago solicitud)
            throws RemoteException, NotBoundException, OperacionRechazadaException {
        ServicioPagos pagos = (ServicioPagos) LocateRegistry.getRegistry(host, puertoPagos)
                .lookup(ServicioPagos.NOMBRE);
        ComprobantePago existente = pagos.consultarPago(solicitud.pedidoId());
        if (existente != null) {
            if (existente.monto().compareTo(solicitud.monto()) != 0
                    || !existente.metodoPago().equals(solicitud.metodoPago())) {
                throw new OperacionRechazadaException("El pago existente no coincide con el pedido");
            }
            return existente;
        }
        return pagos.registrarPago(solicitud);
    }

    public void registrarVenta(ComprobantePago pago) throws RemoteException, NotBoundException {
        ServicioAdministracion admin = (ServicioAdministracion) LocateRegistry
                .getRegistry(host, puertoAdministracion).lookup(ServicioAdministracion.NOMBRE);
        admin.registrarVenta(new Venta(pago.pedidoId(), pago.monto(), pago.fecha()));
    }

    public void enviarACocina(PedidoCocina pedido)
            throws RemoteException, NotBoundException, OperacionRechazadaException {
        ServicioCocina cocina = (ServicioCocina) LocateRegistry.getRegistry(host, puertoCocina)
                .lookup(ServicioCocina.NOMBRE);
        cocina.registrarPedido(pedido);
    }
}
