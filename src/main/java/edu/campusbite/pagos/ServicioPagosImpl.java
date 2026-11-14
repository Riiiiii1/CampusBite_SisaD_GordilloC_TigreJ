package edu.campusbite.pagos;

import edu.campusbite.common.dto.ComprobantePago;
import edu.campusbite.common.dto.SolicitudPago;
import edu.campusbite.common.rmi.OperacionRechazadaException;
import edu.campusbite.common.rmi.ServicioPagos;
import edu.campusbite.common.util.Registro;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
Implementacion del servicio remoto de Pagos y Facturacion.
Semantica: a lo sumo una vez (at-most-once) por pedido. Si Pedidos reintenta
porque su llamada expiro, el cobro y la factura NO se duplican: se devuelve
el comprobante ya registrado con repetido = true.
*/
public class ServicioPagosImpl implements ServicioPagos {

    private static final String SERVICIO = "PAGOS";

    // Un comprobante por pedido. La clave es el pedidoId
    private final ConcurrentHashMap<String, ComprobantePago> pagosPorPedido = new ConcurrentHashMap<>();
    private final AtomicLong secuenciaPago = new AtomicLong();
    private final AtomicLong secuenciaFactura = new AtomicLong();

    // Retraso prueba
    private volatile long retrasoMs = 0;

    // Metodo ajustado con ayuda de IA (Claude): ya no envia la venta a Administracion (la envia Pedidos, flecha [6])
    // y usa el log compartido.
    @Override
    public ComprobantePago registrarPago(SolicitudPago solicitud) throws OperacionRechazadaException {
        validar(solicitud);
        String pedidoId = solicitud.pedidoId();
        long inicio = System.currentTimeMillis();
        Registro.info(SERVICIO, "registrarPago(" + pedidoId + ") recibido");

        simularRetraso();

        // computeIfAbsent es atomico: si llegan dos llamadas iguales a la vez,
        // solo una crea el comprobante y la otra recibe el mismo.
        AtomicBoolean esNuevo = new AtomicBoolean(false);
        ComprobantePago comprobante = pagosPorPedido.computeIfAbsent(pedidoId, id -> {
            esNuevo.set(true);
            return crearComprobante(solicitud);
        });
        long duracion = System.currentTimeMillis() - inicio;

        if (!esNuevo.get()) {
            if (comprobante.monto().compareTo(solicitud.monto()) != 0) {
                Registro.info(SERVICIO, "CONFLICTO " + pedidoId + ": ya pagado con " + comprobante.monto()
                        + ", se pidio " + solicitud.monto());
                throw new OperacionRechazadaException("El pedido " + pedidoId
                        + " ya tiene un pago con un monto distinto (" + comprobante.monto() + ")");
            }
            Registro.info(SERVICIO, "DUPLICADO " + pedidoId + ": se devuelve el comprobante " + comprobante.pagoId()
                    + " sin cobrar de nuevo (" + duracion + " ms)");
            return comprobante.comoRepetido();
        }

        Registro.info(SERVICIO, "COBRO " + comprobante.pagoId() + " + FACTURA " + comprobante.numeroFactura()
                + " para " + pedidoId + " completado en " + duracion + " ms");
        return comprobante;
    }

    @Override
    public ComprobantePago consultarPago(String pedidoId) {
        return pagosPorPedido.get(pedidoId);
    }

    @Override
    public List<ComprobantePago> listarPagos() {
        return new ArrayList<>(pagosPorPedido.values());
    }

    public void setRetrasoMs(long retrasoMs) {
        this.retrasoMs = Math.max(0, retrasoMs);
        Registro.info(SERVICIO, "Simulacion: retraso de registrarPago = " + this.retrasoMs + " ms");
    }

    public long getRetrasoMs() {
        return retrasoMs;
    }

    private void validar(SolicitudPago s) throws OperacionRechazadaException {
        if (s == null || s.pedidoId() == null || s.pedidoId().isBlank()) {
            throw new OperacionRechazadaException("pedidoId es obligatorio");
        }
        if (s.monto() == null || s.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OperacionRechazadaException("El monto debe ser mayor que cero");
        }
    }

    private void simularRetraso() {
        long espera = retrasoMs;
        if (espera <= 0) {
            return;
        }
        Registro.info(SERVICIO, "Simulando servicio lento: esperando " + espera + " ms");
        try {
            Thread.sleep(espera);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private ComprobantePago crearComprobante(SolicitudPago s) {
        String pagoId = "PAG-" + secuenciaPago.incrementAndGet();
        // Formato de factura tipo SRI: establecimiento-punto-secuencial
        String factura = String.format("001-001-%09d", secuenciaFactura.incrementAndGet());
        String metodo = s.metodoPago() == null ? "EFECTIVO" : s.metodoPago();
        return new ComprobantePago(s.pedidoId(), pagoId, factura, s.monto(), metodo, Instant.now(), false);
    }
}