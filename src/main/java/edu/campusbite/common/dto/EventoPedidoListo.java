package edu.campusbite.common.dto;

import java.io.Serializable;
import java.time.Instant;

// --------------------- DAVID ---------
//	RECORD-DATOS de aviso que Cocina enviará a Notificaciones.
public record EventoPedidoListo(String eventoId, String pedidoId, String estudianteId,
                                Instant fecha) implements Serializable { }
