package edu.campusbite.pedidos;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

// --------------------- DAVID ---------

// ESTO ES EL CONTROLADOR QUE EXPONE LOS ENDPOINTS


@RestController
@RequestMapping("/pedidos")
@CrossOrigin(origins = "http://localhost:8081")
public class ApiPedidos {
    private final ServicioPedidos servicio;
// CARGAR EL SERVICIO DE SERVICIO PEDIDOS EN UN CONSTRUCTOR. TAMBIEN SE PODRIA UTILIZAR EL AUTOWIRED DE  LOMBOOK. DE HECHO UTILICEN LOMBOOK CARLOS.
    public ApiPedidos(ServicioPedidos servicio) { this.servicio = servicio; }

    @PostMapping
    public ResponseEntity<VistaPedido> crear(
            @RequestHeader(value = "Idempotency-Key", required = false) String clave,
            @Valid @RequestBody SolicitudPedido solicitud) {
        ServicioPedidos.Resultado resultado = servicio.crear(clave, solicitud);
        return respuesta(resultado.pedido(), resultado.nuevo());
    }

    @GetMapping("/{id}")
    public VistaPedido consultar(@PathVariable String id) { return servicio.consultar(id); }

    @PostMapping("/{id}/reintentar")
    public ResponseEntity<VistaPedido> reintentar(@PathVariable String id) {
        return respuesta(servicio.reintentar(id), false);
    }
// Generado por IA (CLAUDE), en este caso para completar la logica de codigos segun la respuesta como el 202
    // o el 422 cuando es rechazado.

    private ResponseEntity<VistaPedido> respuesta(VistaPedido pedido, boolean nuevo) {
        int estado = pedido.estadoPago() == VistaPedido.EstadoPago.RECHAZADO
                || pedido.estadoPreparacion() == VistaPedido.EstadoPreparacion.RECHAZADO ? 422
                : pedido.pendiente() ? 202 : nuevo ? 201 : 200;
        return ResponseEntity.status(estado).location(URI.create("/pedidos/" + pedido.id())).body(pedido);
    }
}
