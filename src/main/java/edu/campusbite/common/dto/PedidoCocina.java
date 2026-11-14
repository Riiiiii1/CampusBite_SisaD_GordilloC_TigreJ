package edu.campusbite.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

// --------------------- DAVID ---------

// RECORD PARA EL PEDIDO QUE SOLO SE ENVIA DESPUES DE PODER CONFIRMAR EL PAGO
public record PedidoCocina(String pedidoId, String estudianteId, String descripcion,
                           BigDecimal montos, Instant creadoEn) implements Serializable {

}
