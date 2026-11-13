package edu.campusbite.common.rmi;

import edu.campusbite.common.dto.EstadisticasVentas;
import edu.campusbite.common.dto.Venta;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Contrato RMI del Servicio de Administracion (estadisticas de ventas).
 * Lo llama Pagos despues de confirmar un cobro.
 */
public interface ServicioAdministracion extends Remote {

    String NOMBRE = "ServicioAdministracion";

    /**
     * Registra una venta. Es idempotente por pedidoId: si llega dos veces
     * (por un reintento de Pagos) se cuenta una sola vez.
     *
     * @return true si la venta era nueva, false si ya estaba registrada.
     */
    boolean registrarVenta(Venta venta) throws RemoteException;

    EstadisticasVentas obtenerEstadisticas() throws RemoteException;
}
