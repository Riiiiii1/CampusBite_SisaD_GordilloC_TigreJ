package edu.campusbite.pedidos;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Locale;

// --------------------- DAVID ---------
// LA API RECIBE LOS DATOS DE UN PEDIDO DE ESTA FORMA

// PEDI A LA IA (CLAUDE) refactorizar las variables y hacer validaciones basicas como NotBlank cada uno
// Aparte de añadir en el constructor un condicional ternario para haya texto, se eliminen los espacios
// en blanco con el stripp.

public record SolicitudPedido(
        @NotBlank @Size(max = 100) String estudianteId,
        @NotBlank @Size(max = 500) String descripcion,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal monto,
        @NotBlank @Pattern(regexp = "EFECTIVO|TARJETA") String metodoPago) {

    public SolicitudPedido {
        estudianteId = estudianteId == null ? null : estudianteId.strip();
        descripcion = descripcion == null ? null : descripcion.strip();
        monto = monto == null ? null : monto.stripTrailingZeros();
        metodoPago = metodoPago == null ? "EFECTIVO" : metodoPago.strip().toUpperCase(Locale.ROOT);
    }
}
