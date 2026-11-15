package edu.campusbite.notificaciones;

import edu.campusbite.common.dto.EventoPedidoListo;
import edu.campusbite.common.rmi.OperacionRechazadaException;
import edu.campusbite.common.rmi.ServicioNotificaciones;
import edu.campusbite.common.util.Registro;

import java.util.concurrent.ConcurrentHashMap;

/** Acepta eventos RMI y evita procesar dos veces el mismo eventoId */
public class ServicioNotificacionesImpl implements ServicioNotificaciones {

    private static final String SERVICIO = "NOTIFICACIONES";

    private final ConcurrentHashMap<String, EventoPedidoListo> eventosPorId =
            new ConcurrentHashMap<>();

    private final NotificacionesWebSocketHandler manejador;

    public ServicioNotificacionesImpl(
            NotificacionesWebSocketHandler manejador) {
        this.manejador = manejador;
    }

    @Override
    public void notificarListo(EventoPedidoListo evento)
            throws OperacionRechazadaException {
        validar(evento);

        EventoPedidoListo existente =
                eventosPorId.putIfAbsent(evento.eventoId(), evento);

        if (existente != null) {
            if (!existente.equals(evento)) {
                throw new OperacionRechazadaException(
                        "eventoId ya utilizado con datos distintos");
            }

            Registro.info(SERVICIO,
                    "Evento duplicado ignorado: " + evento.eventoId());
            return;
        }

        int clientesAvisados = manejador.enviar(evento);

        Registro.info(SERVICIO,
                "Evento aceptado: " + evento.eventoId()
                        + " | clientes WebSocket avisados: "
                        + clientesAvisados);
    }

    private static void validar(EventoPedidoListo evento)
            throws OperacionRechazadaException {
        if (evento == null) {
            throw new OperacionRechazadaException(
                    "El evento es obligatorio");
        }
        if (evento.eventoId() == null || evento.eventoId().isBlank()) {
            throw new OperacionRechazadaException(
                    "eventoId es obligatorio");
        }
        if (evento.pedidoId() == null || evento.pedidoId().isBlank()) {
            throw new OperacionRechazadaException(
                    "pedidoId es obligatorio");
        }
        if (evento.estudianteId() == null
                || evento.estudianteId().isBlank()) {
            throw new OperacionRechazadaException(
                    "estudianteId es obligatorio");
        }
        if (evento.fecha() == null) {
            throw new OperacionRechazadaException(
                    "La fecha del evento es obligatoria");
        }
    }
}