package edu.campusbite.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

// ------------------------------- MODIFICADO DAVID - ORIGINAL JONH -------------------------------
public record Venta(String pedidoId, BigDecimal monto, Instant fecha) implements Serializable {
}
