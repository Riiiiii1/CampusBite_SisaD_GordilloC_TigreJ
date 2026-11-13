package edu.campusbite.common.rmi;

import edu.campusbite.common.dto.ComprobantePago;
import edu.campusbite.common.dto.SolicitudPago;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Contrato RMI del Servicio de Pagos y Facturacion.
 * Lo implementa pagos-service y lo usa Pedidos como cliente.
 */
public interface ServicioPagos extends Remote {

    /** Nombre con el que se publica en el registro RMI. */
    String NOMBRE = "ServicioPagos";

    /**
     * Registra el cobro de un pedido y emite su factura.
     * Es idempotente por pedidoId: si se repite con el mismo monto devuelve
     * el MISMO comprobante (repetido = true) sin cobrar ni facturar otra vez.
     *
     * @throws OperacionRechazadaException si los datos son invalidos o si el
     *         pedidoId ya existe con un monto distinto.
     */
    ComprobantePago registrarPago(SolicitudPago solicitud)
            throws RemoteException, OperacionRechazadaException;

    /** Devuelve el comprobante de un pedido o null si no tiene pago. */
    ComprobantePago consultarPago(String pedidoId) throws RemoteException;

    /** Lista todos los comprobantes (para caja y facturacion). */
    List<ComprobantePago> listarPagos() throws RemoteException;
}
