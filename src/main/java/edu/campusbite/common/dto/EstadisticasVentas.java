package edu.campusbite.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/** Resumen de ventas para la administracion. */
public record EstadisticasVentas(long cantidadVentas, BigDecimal totalVendido, BigDecimal ticketPromedio)
        implements Serializable {
}
