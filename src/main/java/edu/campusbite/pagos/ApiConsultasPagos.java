package edu.campusbite.pagos;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.campusbite.common.dto.ComprobantePago;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static edu.campusbite.common.http.RespuestaHttp.error;
import static edu.campusbite.common.http.RespuestaHttp.json;
import static edu.campusbite.common.http.RespuestaHttp.texto;

/**
    API REST de consulta para caja y facturacion (servidor HTTP del JDK).
    GET  /caja/pagos                       -> todos los cobros
    GET  /facturacion/facturas/{pedidoId}  -> factura de un pedido
    GET  /pagos/estado                     -> retraso simulado
    POST /simulacion/retraso?ms=5000       -> simula un servicio lento (0 = normal)
 */
public class ApiConsultasPagos {

    private final ServicioPagosImpl servicio;
    private final int puerto;

    public ApiConsultasPagos(ServicioPagosImpl servicio, int puerto) {
        this.servicio = servicio;
        this.puerto = puerto;
    }

    public void iniciar() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(puerto), 0);
        server.createContext("/caja/pagos", this::listarPagos);
        server.createContext("/facturacion/facturas/", this::facturaDePedido);
        server.createContext("/pagos/estado", this::estado);
        server.createContext("/simulacion/retraso", this::cambiarRetraso);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
    }

    private void listarPagos(HttpExchange ex) throws IOException {
        if (!soloMetodo(ex, "GET")) {
            return;
        }
        List<ComprobantePago> pagos = servicio.listarPagos();
        pagos.sort(Comparator.comparing(ComprobantePago::fecha));
        json(ex, 200, pagos.stream().map(ApiConsultasPagos::aJson)
                .collect(Collectors.joining(",", "[", "]")));
    }

    private void facturaDePedido(HttpExchange ex) throws IOException {
        if (!soloMetodo(ex, "GET")) {
            return;
        }
        String pedidoId = ex.getRequestURI().getPath().substring("/facturacion/facturas/".length());
        ComprobantePago c = servicio.consultarPago(pedidoId);
        if (c == null) {
            error(ex, 404, "No existe factura para el pedido " + pedidoId);
        } else {
            json(ex, 200, aJson(c));
        }
    }

    private void estado(HttpExchange ex) throws IOException {
        json(ex, 200, "{\"retrasoMs\":" + servicio.getRetrasoMs() + "}");
    }

    // Metodo corregido con ayuda de IA (Claude): validar que exista ms=... y que sea numero; antes fallaba con query vacia.
    private void cambiarRetraso(HttpExchange ex) throws IOException {
        if (!soloMetodo(ex, "POST")) {
            return;
        }
        String query = ex.getRequestURI().getQuery();
        try {
            if (query == null || !query.startsWith("ms=")) {
                throw new IllegalArgumentException("falta ms");
            }
            servicio.setRetrasoMs(Long.parseLong(query.substring("ms=".length())));
            json(ex, 200, "{\"retrasoMs\":" + servicio.getRetrasoMs() + "}");
        } catch (IllegalArgumentException e) {
            error(ex, 400, "Use /simulacion/retraso?ms=5000");
        }
    }

    private static boolean soloMetodo(HttpExchange ex, String metodo) throws IOException {
        if (!metodo.equals(ex.getRequestMethod())) {
            error(ex, 405, "Metodo no permitido, use " + metodo);
            return false;
        }
        return true;
    }

    static String aJson(ComprobantePago c) {
        return "{\"pedidoId\":" + texto(c.pedidoId())
                + ",\"pagoId\":" + texto(c.pagoId())
                + ",\"numeroFactura\":" + texto(c.numeroFactura())
                + ",\"monto\":" + c.monto().toPlainString()
                + ",\"metodoPago\":" + texto(c.metodoPago())
                + ",\"fecha\":" + texto(c.fecha()) + "}";
    }
}