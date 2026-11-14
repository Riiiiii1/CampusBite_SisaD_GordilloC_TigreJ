package edu.campusbite.pedidos;

import edu.campusbite.common.dto.ComprobantePago;
import edu.campusbite.common.dto.SolicitudPago;
import edu.campusbite.common.dto.PedidoCocina;
import edu.campusbite.common.rmi.OperacionRechazadaException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import static edu.campusbite.pedidos.VistaPedido.EstadoPago.*;
import edu.campusbite.pedidos.VistaPedido.EstadoPreparacion;

// --------------------- DAVID ---------
@Service
public class ServicioPedidos {
    private final ConcurrentHashMap<String, Pedido> porClave = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Pedido> porId = new ConcurrentHashMap<>();
    private final Semaphore llamadasDisponibles = new Semaphore(8);
    private final ClientesRmi clientes;
    private final boolean cocinaHabilitada;

    public ServicioPedidos(ClientesRmi clientes,
                           @Value("${campusbite.cocina.habilitada:false}") boolean cocinaHabilitada) {
        this.clientes = clientes;
        this.cocinaHabilitada = cocinaHabilitada;
    }

    public record Resultado(VistaPedido pedido, boolean nuevo) { }

    private static class Pedido {
        final ReentrantLock bloqueo = new ReentrantLock();
        volatile VistaPedido vista;
        volatile boolean envioIntentado;

        Pedido(SolicitudPedido solicitud, boolean cocinaHabilitada) {
            vista = new VistaPedido(UUID.randomUUID().toString(), solicitud, PENDIENTE_PAGO,
                    false, false, "Pendiente de confirmar pago", null, Instant.now(),
                    cocinaHabilitada ? EstadoPreparacion.PENDIENTE_ENVIO : EstadoPreparacion.PENDIENTE_INTEGRACION,
                    cocinaHabilitada ? "Se enviara cuando se confirme el pago" : "Integracion con Cocina desactivada");
        }

        synchronized void actualizar(VistaPedido.EstadoPago estado, boolean venta, boolean procesando,
                        String detalle, ComprobantePago comprobante) {
            vista = new VistaPedido(vista.id(), vista.solicitud(), estado, venta,
                    procesando, detalle, comprobante, vista.creadoEn(), vista.estadoPreparacion(), vista.detalleCocina());
        }

        synchronized void preparacion(EstadoPreparacion estado, String detalle) {
            if (vista.estadoPreparacion() == EstadoPreparacion.LISTO) return;
            vista = new VistaPedido(vista.id(), vista.solicitud(), vista.estadoPago(), vista.ventaRegistrada(),
                    vista.procesando(), vista.detalle(), vista.comprobante(), vista.creadoEn(), estado, detalle);
        }
    }

    public Resultado crear(String clave, SolicitudPedido solicitud) {
        if (clave == null || clave.isBlank() || clave.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Idempotency-Key es obligatorio y debe tener entre 1 y 128 caracteres");
        }
        Pedido candidato = new Pedido(solicitud, cocinaHabilitada);
        Pedido pedido = porClave.computeIfAbsent(clave, k -> {
            porId.put(candidato.vista.id(), candidato);
            return candidato;
        });
        if (!pedido.vista.solicitud().equals(solicitud)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Idempotency-Key ya utilizado con otro contenido");
        }
        boolean nuevo = pedido == candidato;
        if (nuevo) procesar(pedido);
        return new Resultado(pedido.vista, nuevo);
    }

    public VistaPedido consultar(String id) { return buscar(id).vista; }

    public VistaPedido reintentar(String id) {
        Pedido pedido = buscar(id);
        procesar(pedido);
        return pedido.vista;
    }

    public boolean marcarListo(String id) throws OperacionRechazadaException {
        Pedido pedido = id == null ? null : porId.get(id);
        if (pedido == null) throw new OperacionRechazadaException("Pedido no encontrado");
        synchronized (pedido) {
            if (pedido.vista.estadoPreparacion() == EstadoPreparacion.LISTO) return false;
            if (pedido.vista.estadoPago() != PAGADO || !pedido.envioIntentado
                    || pedido.vista.estadoPreparacion() == EstadoPreparacion.RECHAZADO) {
                throw new OperacionRechazadaException("El pedido no tiene un envio valido a Cocina");
            }
            pedido.preparacion(EstadoPreparacion.LISTO, "Cocina confirmo que el pedido esta listo");
            return true;
        }
    }

    private Pedido buscar(String id) {
        Pedido pedido = porId.get(id);
        if (pedido == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado");
        return pedido;
    }

    private void procesar(Pedido pedido) {
        if (!pedido.bloqueo.tryLock()) return;
        boolean permiso = false;
        try {
            if (!pedido.vista.pendiente()) return;
            permiso = llamadasDisponibles.tryAcquire();
            if (!permiso) {
                pedido.actualizar(pedido.vista.estadoPago(), pedido.vista.ventaRegistrada(), false,
                        "Capacidad temporalmente ocupada; reintente este pedido", pedido.vista.comprobante());
                return;
            }
            pedido.actualizar(pedido.vista.estadoPago(), pedido.vista.ventaRegistrada(), true,
                    "Procesando", pedido.vista.comprobante());
            if (pedido.vista.comprobante() == null) {
                SolicitudPedido s = pedido.vista.solicitud();
                ComprobantePago pago = clientes.pagar(new SolicitudPago(pedido.vista.id(), s.monto(), s.metodoPago()));
                pedido.actualizar(PAGADO, false, true, "Pago confirmado; registrando venta", pago);
            }
            if (!pedido.vista.ventaRegistrada()) {
                try {
                    clientes.registrarVenta(pedido.vista.comprobante());
                    pedido.actualizar(PAGADO, true, true, "Pago y venta registrados", pedido.vista.comprobante());
                } catch (RemoteException | NotBoundException | IllegalArgumentException e) {
                    pedido.actualizar(PAGADO, false, true,
                            "Pago confirmado; registro de venta pendiente. Reintente este mismo pedido",
                            pedido.vista.comprobante());
                }
            }
            enviarACocina(pedido);
            pedido.actualizar(pedido.vista.estadoPago(), pedido.vista.ventaRegistrada(), false,
                    pedido.vista.detalle(), pedido.vista.comprobante());
        } catch (OperacionRechazadaException e) {
            pedido.actualizar(RECHAZADO, false, false, e.getMessage(), pedido.vista.comprobante());
        } catch (RemoteException | NotBoundException | IllegalArgumentException e) {
            String detalle = pedido.vista.comprobante() == null
                    ? "Pago sin confirmar: puede haberse realizado. Reintente este mismo pedido"
                    : "Pago confirmado; registro de venta pendiente. Reintente este mismo pedido";
            pedido.actualizar(pedido.vista.estadoPago(), pedido.vista.ventaRegistrada(), false,
                    detalle, pedido.vista.comprobante());
        } finally {
            if (permiso) llamadasDisponibles.release();
            pedido.bloqueo.unlock();
        }
    }

    private void enviarACocina(Pedido pedido) {
        if (!cocinaHabilitada || pedido.vista.estadoPreparacion() != EstadoPreparacion.PENDIENTE_ENVIO) return;
        pedido.envioIntentado = true;
        SolicitudPedido s = pedido.vista.solicitud();
        try {
            clientes.enviarACocina(new PedidoCocina(pedido.vista.id(), s.estudianteId(), s.descripcion(),
                    s.monto(), pedido.vista.creadoEn()));
            pedido.preparacion(EstadoPreparacion.RECIBIDO, "Cocina confirmo la recepcion");
        } catch (OperacionRechazadaException e) {
            pedido.preparacion(EstadoPreparacion.RECHAZADO, "Cocina rechazo el pedido: " + e.getMessage());
        } catch (RemoteException | NotBoundException e) {
            pedido.preparacion(EstadoPreparacion.PENDIENTE_ENVIO,
                    "Recepcion de Cocina sin confirmar; reintente el mismo pedido");
        }
    }
}
