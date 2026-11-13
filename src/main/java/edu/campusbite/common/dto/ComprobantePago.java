package edu.campusbite.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Resultado de registrar un pago: cobro + factura.
 * repetido = true indica que la solicitud ya se habia procesado antes
 * (reintento) y se devolvio el mismo comprobante sin cobrar de nuevo.
 */
public record ComprobantePago(
        String pedidoId,
        String pagoId,
        String numeroFactura,
        BigDecimal monto,
        String metodoPago,
        Instant fecha,
        boolean repetido) implements Serializable {

    public ComprobantePago comoRepetido() {
        return new ComprobantePago(pedidoId, pagoId, numeroFactura, monto, metodoPago, fecha, true);
    }
}
