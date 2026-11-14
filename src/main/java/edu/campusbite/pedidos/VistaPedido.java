package edu.campusbite.pedidos;

import edu.campusbite.common.dto.ComprobantePago;
import java.time.Instant;

// --------------------- DAVID ---------

//	Definir respuesta como el  identificador, pago, venta, preparación y comprobante.
public record VistaPedido(String id, SolicitudPedido solicitud, EstadoPago estadoPago,
                          boolean ventaRegistrada, boolean procesando, String detalle,
                          ComprobantePago comprobante, Instant creadoEn,
                          EstadoPreparacion estadoPreparacion, String detalleCocina) {
    public enum EstadoPago { PENDIENTE_PAGO, PAGADO, RECHAZADO }
    public enum EstadoPreparacion { PENDIENTE_INTEGRACION, PENDIENTE_ENVIO, RECIBIDO, LISTO, RECHAZADO }
//En caso de que el pago no fue rechazado devulve TRUE, es decir pendiente.
    public boolean pendiente() {
        return estadoPago != EstadoPago.RECHAZADO
                && (procesando || estadoPago == EstadoPago.PENDIENTE_PAGO || !ventaRegistrada
                || estadoPreparacion == EstadoPreparacion.PENDIENTE_ENVIO);
    }
}
