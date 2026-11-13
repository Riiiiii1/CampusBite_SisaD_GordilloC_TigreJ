package edu.campusbite.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/** Datos que Pedidos envia a Pagos. Viaja serializado por RMI. */
public record SolicitudPago(String pedidoId, BigDecimal monto, String metodoPago)
        implements Serializable {
}
