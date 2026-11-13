package edu.campusbite.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/** Venta que Pagos reporta a Administracion. */
public record Venta(String pedidoId, BigDecimal monto, Instant fecha) implements Serializable {
}
