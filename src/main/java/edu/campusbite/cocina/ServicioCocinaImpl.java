package edu.campusbite.cocina;

import edu.campusbite.common.dto.EventoPedidoListo;
import edu.campusbite.common.dto.PedidoCocina;
import edu.campusbite.common.rmi.ConfiguracionRmi;
import edu.campusbite.common.rmi.OperacionRechazadaException;
import edu.campusbite.common.rmi.ServicioCocina;
import edu.campusbite.common.rmi.ServicioEstadoPedidos;
import edu.campusbite.common.rmi.ServicioNotificaciones;
import edu.campusbite.common.util.Registro;

import java.math.BigDecimal;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recibe pedidos por RMI y coordina el cambio a LISTO con Pedidos
 * y el aviso posterior a notificaciones
 */
public class ServicioCocinaImpl implements ServicioCocina {

    private static final String SERVICIO = "COCINA";

    private final ConcurrentHashMap<String, PedidoEnCocina> pedidos =
            new ConcurrentHashMap<>();

    @Override
    public void registrarPedido(PedidoCocina pedido)
            throws OperacionRechazadaException {
        validar(pedido);

        PedidoEnCocina nuevo = new PedidoEnCocina(pedido);
        PedidoEnCocina existente = pedidos.putIfAbsent(pedido.pedidoId(), nuevo);

        if (existente != null && !mismosDatos(existente.pedido, pedido)) {
            throw new OperacionRechazadaException(
                    "El pedido ya existe con datos distintos");
        }

        if (existente == null) {
            Registro.info(SERVICIO, "Pedido recibido: " + pedido.pedidoId());
        } else {
            Registro.info(SERVICIO, "Pedido duplicado recibido; se conserva: "
                    + pedido.pedidoId());
        }
    }

    /**
     * Marca el pedido como listo en Pedidos antes de solicitar el aviso
     * Si el aviso falla, una neuva llamada reutiliza el mismo evento
     */
    public ResultadoListo marcarListo(String pedidoId)
            throws RemoteException, OperacionRechazadaException {
        if (pedidoId == null || pedidoId.isBlank()) {
            throw new OperacionRechazadaException("pedidoId es obligatorio");
        }

        PedidoEnCocina pedido = pedidos.get(pedidoId);
        if (pedido == null) {
            throw new OperacionRechazadaException(
                    "No existe el pedido en Cocina: " + pedidoId);
        }

        synchronized (pedido) {
            if (pedido.notificacionEnviada) {
                return new ResultadoListo(
                        pedido.cambioEstado,
                        pedido.eventoListo.eventoId());
            }

            if (pedido.eventoListo == null) {
                pedido.eventoListo = new EventoPedidoListo(
                        "LISTO:" + pedidoId,
                        pedidoId,
                        pedido.pedido.estudianteId(),
                        Instant.now());
            }

            try {
                if (!pedido.estadoConfirmado) {
                    ServicioEstadoPedidos pedidosRemotos =
                            (ServicioEstadoPedidos) LocateRegistry
                                    .getRegistry(
                                            ConfiguracionRmi.HOST,
                                            ConfiguracionRmi.PUERTO_ESTADO_PEDIDOS)
                                    .lookup(ServicioEstadoPedidos.NOMBRE);

                    pedido.cambioEstado =
                            pedidosRemotos.marcarListo(pedidoId);
                    pedido.estadoConfirmado = true;
                }

                ServicioNotificaciones notificaciones =
                        (ServicioNotificaciones) LocateRegistry
                                .getRegistry(
                                        ConfiguracionRmi.HOST,
                                        ConfiguracionRmi.PUERTO_NOTIFICACIONES)
                                .lookup(ServicioNotificaciones.NOMBRE);

                notificaciones.notificarListo(pedido.eventoListo);
                pedido.notificacionEnviada = true;

                Registro.info(SERVICIO, "Pedido LISTO y notificación aceptada: "
                        + pedidoId);

                return new ResultadoListo(
                        pedido.cambioEstado,
                        pedido.eventoListo.eventoId());

            } catch (NotBoundException e) {
                throw new RemoteException(
                        "No se encontró el servicio RMI requerido", e);
            }
        }
    }

    private static void validar(PedidoCocina pedido)
            throws OperacionRechazadaException {
        if (pedido == null) {
            throw new OperacionRechazadaException(
                    "El pedido es obligatorio");
        }
        if (pedido.pedidoId() == null || pedido.pedidoId().isBlank()) {
            throw new OperacionRechazadaException(
                    "pedidoId es obligatorio");
        }
        if (pedido.estudianteId() == null
                || pedido.estudianteId().isBlank()) {
            throw new OperacionRechazadaException(
                    "estudianteId es obligatorio");
        }
        if (pedido.descripcion() == null || pedido.descripcion().isBlank()) {
            throw new OperacionRechazadaException(
                    "La descripción del pedido es obligatoria");
        }
        if (pedido.montos() == null
                || pedido.montos().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OperacionRechazadaException(
                    "El monto debe ser mayor que cero");
        }
        if (pedido.creadoEn() == null) {
            throw new OperacionRechazadaException(
                    "La fecha de creación es obligatoria");
        }
    }

    private static boolean mismosDatos(
            PedidoCocina primero,
            PedidoCocina segundo) {
        return Objects.equals(primero.pedidoId(), segundo.pedidoId())
                && Objects.equals(primero.estudianteId(), segundo.estudianteId())
                && Objects.equals(primero.descripcion(), segundo.descripcion())
                && primero.montos().compareTo(segundo.montos()) == 0
                && Objects.equals(primero.creadoEn(), segundo.creadoEn());
    }

    public record ResultadoListo(boolean cambioEstado, String eventoId) { }

    private static final class PedidoEnCocina {
        private final PedidoCocina pedido;
        private EventoPedidoListo eventoListo;
        private boolean estadoConfirmado;
        private boolean cambioEstado;
        private boolean notificacionEnviada;

        private PedidoEnCocina(PedidoCocina pedido) {
            this.pedido = pedido;
        }
    }
}