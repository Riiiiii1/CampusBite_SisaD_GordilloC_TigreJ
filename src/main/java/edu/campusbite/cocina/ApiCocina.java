package edu.campusbite.cocina;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.campusbite.common.dto.PedidoCocina;
import edu.campusbite.common.rmi.OperacionRechazadaException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static edu.campusbite.common.http.RespuestaHttp.error;
import static edu.campusbite.common.http.RespuestaHttp.json;
import static edu.campusbite.common.http.RespuestaHttp.texto;

/**
 * API HTTP para que el personal de cocina marque pedidos como listos
 * POST /cocina/pedidos/{pedidoId}/listo
 */
public class ApiCocina {

    private static final String PREFIJO = "/cocina/pedidos/";
    private static final String SUFIJO = "/listo";
    private static final String ORIGEN_PAGINA =
            "http://localhost:8081";

    private final ServicioCocinaImpl servicio;
    private final int puerto;

    private HttpServer server;
    private ExecutorService executor;

    public ApiCocina(ServicioCocinaImpl servicio, int puerto) {
        this.servicio = servicio;
        this.puerto = puerto;
    }

    public synchronized void iniciar() throws IOException {
        if (server != null) {
            return;
        }

        server = HttpServer.create(new InetSocketAddress(puerto), 0);
        server.createContext(PREFIJO, this::marcarListo);
        server.createContext("/cocina/pedidos", this::listarPedidos);

        executor = Executors.newFixedThreadPool(4);
        server.setExecutor(executor);
        server.start();
    }

    public synchronized void detener() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void listarPedidos(HttpExchange ex) throws IOException {
        agregarCORS(ex);

        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.getResponseHeaders().set(
                    "Access-Control-Allow-Methods", "GET, OPTIONS");
            ex.getResponseHeaders().set(
                    "Access-Control-Allow-Headers", "Content-Type");
            ex.getResponseHeaders().set(
                    "Access-Control-Max-Age", "600");
            ex.sendResponseHeaders(204, -1);
            ex.close();
            return;
        }

        if (!soloMetodo(ex, "GET")) {
            return;
        }

        List<ServicioCocinaImpl.PedidoResumen> pedidos =
                servicio.listarPedidos();

        String contenido = pedidos.stream()
                .map(ApiCocina::aJson)
                .collect(Collectors.joining(",", "[", "]"));

        json(ex, 200, contenido);
    }

    private static String aJson(
            ServicioCocinaImpl.PedidoResumen resumen) {
        PedidoCocina pedido = resumen.pedido();

        return "{\"pedidoId\":" + texto(pedido.pedidoId())
                + ",\"estudianteId\":" + texto(pedido.estudianteId())
                + ",\"descripcion\":" + texto(pedido.descripcion())
                + ",\"montos\":" + pedido.montos().toPlainString()
                + ",\"creadoEn\":" + texto(pedido.creadoEn().toString())
                + ",\"estado\":" + texto(resumen.estado())
                + "}";
    }

    private void marcarListo(HttpExchange ex) throws IOException {
        agregarCORS(ex);

        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.getResponseHeaders().set(
                    "Access-Control-Allow-Methods", "POST, OPTIONS");
            ex.getResponseHeaders().set(
                    "Access-Control-Allow-Headers", "Content-Type");
            ex.getResponseHeaders().set(
                    "Access-Control-Max-Age", "600");
            ex.sendResponseHeaders(204, -1);
            ex.close();
            return;
        }

        if (!soloMetodo(ex, "POST")) {
            return;
        }

        String ruta = ex.getRequestURI().getPath();
        if (!ruta.startsWith(PREFIJO) || !ruta.endsWith(SUFIJO)) {
            error(ex, 404, "Use /cocina/pedidos/{pedidoId}/listo");
            return;
        }

        String pedidoId = ruta.substring(
                PREFIJO.length(),
                ruta.length() - SUFIJO.length());

        if (pedidoId.isBlank() || pedidoId.contains("/")) {
            error(ex, 400, "pedidoId no es válido");
            return;
        }

        try {
            ServicioCocinaImpl.ResultadoListo resultado =
                    servicio.marcarListo(pedidoId);

            json(ex, 200,
                    "{\"pedidoId\":" + texto(pedidoId)
                            + ",\"estado\":\"LISTO\""
                            + ",\"estadoCambio\":" + resultado.cambioEstado()
                            + ",\"eventoId\":" + texto(resultado.eventoId())
                            + "}");

        } catch (OperacionRechazadaException e) {
            int estadoHttp = e.getMessage() != null
                    && e.getMessage().startsWith("No existe el pedido")
                    ? 404
                    : 409;
            error(ex, estadoHttp, e.getMessage());

        } catch (RemoteException e) {
            error(ex, 503,
                    "No se pudo completar la operación remota. "
                            + "Puedes reintentar la misma solicitud.");
        }
    }

    private static void agregarCORS(HttpExchange ex) {
        String origen = ex.getRequestHeaders().getFirst("Origin");

        if (ORIGEN_PAGINA.equals(origen)) {
            Headers cabeceras = ex.getResponseHeaders();
            cabeceras.set("Access-Control-Allow-Origin", ORIGEN_PAGINA);
            cabeceras.set("Vary", "Origin");
        }
    }

    private static boolean soloMetodo(
            HttpExchange ex,
            String metodo) throws IOException {
        if (!metodo.equals(ex.getRequestMethod())) {
            error(ex, 405, "Método no permitido; use " + metodo);
            return false;
        }
        return true;
    }
}