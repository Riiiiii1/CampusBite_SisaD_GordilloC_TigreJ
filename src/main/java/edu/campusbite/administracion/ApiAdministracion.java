package edu.campusbite.administracion;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.campusbite.common.dto.EstadisticasVentas;
import edu.campusbite.common.dto.Venta;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static edu.campusbite.common.http.RespuestaHttp.json;
import static edu.campusbite.common.http.RespuestaHttp.texto;

/**
    API REST para el personal de administracion.
    GET /admin/estadisticas => cantidad de ventas, total y ticket promedio
    GET /admin/ventas       => detalle de ventas
 */
public class ApiAdministracion {

    private final ServicioAdministracionImpl servicio;
    private final int puerto;

    public ApiAdministracion(ServicioAdministracionImpl servicio, int puerto) {
        this.servicio = servicio;
        this.puerto = puerto;
    }

    public void iniciar() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(puerto), 0);
        server.createContext("/admin/estadisticas", this::estadisticas);
        server.createContext("/admin/ventas", this::ventas);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
    }

    private void estadisticas(HttpExchange ex) throws IOException {
        EstadisticasVentas e = servicio.obtenerEstadisticas();
        json(ex, 200, "{\"cantidadVentas\":" + e.cantidadVentas()
                + ",\"totalVendido\":" + e.totalVendido().toPlainString()
                + ",\"ticketPromedio\":" + e.ticketPromedio().toPlainString() + "}");
    }

    private void ventas(HttpExchange ex) throws IOException {
        List<Venta> ventas = servicio.listarVentas();
        ventas.sort(Comparator.comparing(Venta::fecha));
        json(ex, 200, ventas.stream()
                .map(v -> "{\"pedidoId\":" + texto(v.pedidoId())
                        + ",\"monto\":" + v.monto().toPlainString()
                        + ",\"fecha\":" + texto(v.fecha()) + "}")
                .collect(Collectors.joining(",", "[", "]")));
    }
}