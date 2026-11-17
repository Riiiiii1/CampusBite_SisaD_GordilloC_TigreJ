package edu.campusbite.pedidos;

import edu.campusbite.administracion.ServicioAdministracionImpl;
import edu.campusbite.cocina.ServicioCocinaImpl;
import edu.campusbite.pagos.ServicioPagosImpl;
import edu.campusbite.common.dto.Venta;
import edu.campusbite.common.http.RespuestaHttp;
import edu.campusbite.common.rmi.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;

// --------------------- DAVID ---------
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "campusbite.cocina.habilitada=true")
@DirtiesContext
class IntegracionPedidosTest {
    @LocalServerPort int puertoHttp;
    static Registry registroPagos;
    static Registry registroAdmin;
    static Registry registroCocina;
    static final ServicioPagosImpl pagos = new ServicioPagosImpl();
    static final ServicioAdministracionImpl admin = new ServicioAdministracionImpl();
    static final ServicioCocinaImpl cocina = new ServicioCocinaImpl();
    static ServicioPagos stubPagos;
    static ServicioAdministracion stubAdmin;
    static ServicioCocina stubCocina;
    final HttpClient http = HttpClient.newHttpClient();
    final ObjectMapper json = new ObjectMapper();
    static final String CUERPO = """
            {"estudianteId":"est-1","descripcion":"Almuerzo","monto":4.50,"metodoPago":"EFECTIVO"}
            """;

    @DynamicPropertySource
    static void servicios(DynamicPropertyRegistry propiedades) throws Exception {
        ConfiguracionRmi.configurarTimeoutsCliente(2000, 3000);
        System.setProperty("java.rmi.server.hostname", "localhost");
        int puertoPagos = puertoLibre();
        registroPagos = LocateRegistry.createRegistry(puertoPagos);
        int puertoAdmin = puertoLibre();
        registroAdmin = LocateRegistry.createRegistry(puertoAdmin);
        int puertoCocina = puertoLibre();
        registroCocina = LocateRegistry.createRegistry(puertoCocina);
        int puertoEstadoPedidos = puertoLibre();
        stubPagos = (ServicioPagos) UnicastRemoteObject.exportObject(pagos, 0);
        stubAdmin = (ServicioAdministracion) UnicastRemoteObject.exportObject(admin, 0);
        stubCocina = (ServicioCocina) UnicastRemoteObject.exportObject(cocina, 0);
        registroPagos.rebind(ServicioPagos.NOMBRE, stubPagos);
        registroAdmin.rebind(ServicioAdministracion.NOMBRE, stubAdmin);
        registroCocina.rebind(ServicioCocina.NOMBRE, stubCocina);
        propiedades.add("campusbite.rmi.pagos", () -> puertoPagos);
        propiedades.add("campusbite.rmi.administracion", () -> puertoAdmin);
        propiedades.add("campusbite.rmi.cocina", () -> puertoCocina);
        propiedades.add("campusbite.rmi.estado-pedidos", () -> puertoEstadoPedidos);
    }

    static int puertoLibre() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) { return socket.getLocalPort(); }
    }

    @AfterAll
    static void cerrar() throws Exception {
        UnicastRemoteObject.unexportObject(pagos, true);
        UnicastRemoteObject.unexportObject(admin, true);
        UnicastRemoteObject.unexportObject(cocina, true);
        UnicastRemoteObject.unexportObject(registroPagos, true);
        UnicastRemoteObject.unexportObject(registroAdmin, true);
        UnicastRemoteObject.unexportObject(registroCocina, true);
    }

    HttpRequest post(String ruta, String clave, String cuerpo) {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + puertoHttp + ruta))
                .timeout(Duration.ofSeconds(12)).header("Content-Type", "application/json");
        if (clave != null) builder.header("Idempotency-Key", clave);
        return builder.POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build();
    }

    HttpResponse<String> enviar(HttpRequest request) throws Exception {
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    JsonNode contenido(HttpResponse<String> respuesta) { return json.readTree(respuesta.body()); }

    @Test
    void duplicadosConcurrentesYValidacionRest() throws Exception {
        int pagosAntes = pagos.listarPagos().size();
        long ventasAntes = admin.obtenerEstadisticas().cantidadVentas();
        String clave = UUID.randomUUID().toString();
        List<CompletableFuture<HttpResponse<String>>> solicitudes = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            solicitudes.add(http.sendAsync(post("/pedidos", clave, CUERPO), HttpResponse.BodyHandlers.ofString()));
        }
        CompletableFuture.allOf(solicitudes.toArray(new CompletableFuture<?>[0])).join();
        Set<String> ids = new HashSet<>();
        for (var solicitud : solicitudes) {
            var respuesta = solicitud.join();
            assertTrue(Set.of(200, 201, 202).contains(respuesta.statusCode()), respuesta.body());
            ids.add(contenido(respuesta).get("id").asString());
        }
        assertEquals(1, ids.size());
        assertEquals(pagosAntes + 1, pagos.listarPagos().size());
        assertEquals(ventasAntes + 1, admin.obtenerEstadisticas().cantidadVentas());
        var repetida = enviar(post("/pedidos", clave, CUERPO));
        assertEquals(200, repetida.statusCode());
        String id = ids.iterator().next();
        assertEquals(id, contenido(repetida).get("id").asString());
        assertEquals("RECIBIDO", contenido(repetida).get("estadoPreparacion").asString());
        assertEquals(1, cocina.listarPedidos().stream().filter(p -> p.pedido().pedidoId().equals(id)).count());
        assertEquals(409, enviar(post("/pedidos", clave, CUERPO.replace("4.50", "8.00"))).statusCode());
        assertEquals(400, enviar(post("/pedidos", null, CUERPO)).statusCode());
        assertEquals(400, enviar(post("/pedidos", "invalido", CUERPO.replace("4.50", "-1"))).statusCode());
        assertEquals(400, enviar(post("/pedidos", "json-roto", "{")).statusCode());
        assertEquals(404, enviar(HttpRequest.newBuilder(URI.create("http://localhost:" + puertoHttp + "/pedidos/no-existe")).GET().build()).statusCode());
    }

    @Test
    void pagoTardioSeRecuperaSinDuplicar() throws Exception {
        pagos.setRetrasoMs(3500);
        String id;
        try {
            long inicio = System.nanoTime();
            var respuesta = enviar(post("/pedidos", UUID.randomUUID().toString(), CUERPO));
            assertEquals(202, respuesta.statusCode(), respuesta.body());
            assertTrue(Duration.ofNanos(System.nanoTime() - inicio).toMillis() < 8000);
            id = contenido(respuesta).get("id").asString();
            assertEquals("PENDIENTE_PAGO", contenido(respuesta).get("estadoPago").asString());
            var consulta = enviar(HttpRequest.newBuilder(URI.create("http://localhost:" + puertoHttp + "/pedidos/" + id)).GET().build());
            assertEquals(200, consulta.statusCode());
            long limite = System.nanoTime() + Duration.ofSeconds(4).toNanos();
            while (pagos.consultarPago(id) == null && System.nanoTime() < limite) Thread.sleep(50);
            assertNotNull(pagos.consultarPago(id), "El servidor debe completar el pago despues del timeout");
        } finally {
            pagos.setRetrasoMs(0);
        }
        String pagoId = pagos.consultarPago(id).pagoId();
        var recuperada = enviar(post("/pedidos/" + id + "/reintentar", null, ""));
        assertEquals(200, recuperada.statusCode(), recuperada.body());
        assertEquals(pagoId, contenido(recuperada).get("comprobante").get("pagoId").asString());
        assertTrue(contenido(recuperada).get("ventaRegistrada").asBoolean());
        assertEquals(1, pagos.listarPagos().stream().filter(p -> p.pedidoId().equals(id)).count());
    }

    @Test
    void administracionCaidaNoRepiteElPago() throws Exception {
        registroAdmin.unbind(ServicioAdministracion.NOMBRE);
        String id;
        String pagoId;
        try {
            var respuesta = enviar(post("/pedidos", UUID.randomUUID().toString(), CUERPO));
            assertEquals(202, respuesta.statusCode());
            var pedido = contenido(respuesta);
            id = pedido.get("id").asString();
            pagoId = pedido.get("comprobante").get("pagoId").asString();
            assertEquals("PAGADO", pedido.get("estadoPago").asString());
            assertFalse(pedido.get("ventaRegistrada").asBoolean());
        } finally {
            registroAdmin.rebind(ServicioAdministracion.NOMBRE, stubAdmin);
        }
        var respuesta = enviar(post("/pedidos/" + id + "/reintentar", null, ""));
        assertEquals(200, respuesta.statusCode());
        assertEquals(pagoId, contenido(respuesta).get("comprobante").get("pagoId").asString());
        assertEquals(1, admin.listarVentas().stream().filter(v -> v.pedidoId().equals(id)).count());
    }

    @Test
    void pagosDetenidoPermiteConsultarYRecuperarElPedido() throws Exception {
        UnicastRemoteObject.unexportObject(pagos, true);
        String id;
        try {
            var respuesta = enviar(post("/pedidos", UUID.randomUUID().toString(), CUERPO));
            assertEquals(202, respuesta.statusCode());
            id = contenido(respuesta).get("id").asString();
            assertEquals("PENDIENTE_PAGO", contenido(respuesta).get("estadoPago").asString());
            assertEquals(200, enviar(HttpRequest.newBuilder(URI.create("http://localhost:" + puertoHttp + "/pedidos/" + id)).GET().build()).statusCode());
        } finally {
            stubPagos = (ServicioPagos) UnicastRemoteObject.exportObject(pagos, 0);
            registroPagos.rebind(ServicioPagos.NOMBRE, stubPagos);
        }
        assertEquals(200, enviar(post("/pedidos/" + id + "/reintentar", null, "")).statusCode());
    }

    @Test
    void cocinaNoDisponibleDevuelve202YSeRecuperaSinDuplicar() throws Exception {
        registroCocina.unbind(ServicioCocina.NOMBRE);
        String id;
        String pagoId;
        try {
            var respuesta = enviar(post("/pedidos", UUID.randomUUID().toString(), CUERPO));
            assertEquals(202, respuesta.statusCode(), respuesta.body());
            var pendiente = contenido(respuesta);
            id = pendiente.get("id").asString();
            pagoId = pendiente.get("comprobante").get("pagoId").asString();
            assertEquals("PAGADO", pendiente.get("estadoPago").asString());
            assertTrue(pendiente.get("ventaRegistrada").asBoolean());
            assertEquals("PENDIENTE_ENVIO", pendiente.get("estadoPreparacion").asString());
            assertTrue(cocina.listarPedidos().stream().noneMatch(p -> p.pedido().pedidoId().equals(id)));
        } finally {
            registroCocina.rebind(ServicioCocina.NOMBRE, stubCocina);
        }

        var respuesta = enviar(post("/pedidos/" + id + "/reintentar", null, ""));
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        var recuperado = contenido(respuesta);
        assertEquals(id, recuperado.get("id").asString());
        assertEquals(pagoId, recuperado.get("comprobante").get("pagoId").asString());
        assertEquals("RECIBIDO", recuperado.get("estadoPreparacion").asString());
        assertEquals(1, pagos.listarPagos().stream().filter(p -> p.pedidoId().equals(id)).count());
        assertEquals(1, admin.listarVentas().stream().filter(v -> v.pedidoId().equals(id)).count());
        assertEquals(1, cocina.listarPedidos().stream().filter(p -> p.pedido().pedidoId().equals(id)).count());
    }

    @Test
    void navegadorDeNotificacionesPuedeConsultarPedidos() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + puertoHttp + "/pedidos"))
                .header("Origin", "http://localhost:8081")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,idempotency-key")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
        var respuesta = enviar(request);
        assertEquals(200, respuesta.statusCode());
        assertEquals("http://localhost:8081", respuesta.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
    }

    @Test
    void validacionDeVentasYJson() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> stubAdmin.registrarVenta(new Venta("invalida", new BigDecimal("-1"), Instant.now())));
        String id = UUID.randomUUID().toString();
        assertTrue(stubAdmin.registrarVenta(new Venta(id, new BigDecimal("5.00"), Instant.now())));
        assertFalse(stubAdmin.registrarVenta(new Venta(id, new BigDecimal("5.0"), Instant.now())));
        assertThrows(IllegalArgumentException.class,
                () -> stubAdmin.registrarVenta(new Venta(id, new BigDecimal("8.00"), Instant.now())));
        String texto = "linea 1\nlinea 2\t\"fin\"";
        assertEquals(texto, json.readTree(RespuestaHttp.texto(texto)).asString());
    }
}
