package edu.campusbite.pedidos;

import edu.campusbite.common.dto.*;
import edu.campusbite.common.rmi.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.time.Instant;
import static edu.campusbite.pedidos.VistaPedido.EstadoPreparacion.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// --------------------- DAVID ---------
class ContratoCocinaTest {
    private final SolicitudPedido solicitud = new SolicitudPedido("est-1", "Almuerzo",
            new BigDecimal("4.50"), "EFECTIVO");

    private ClientesRmi clientes() throws Exception {
        ClientesRmi clientes = mock(ClientesRmi.class);
        when(clientes.pagar(any())).thenAnswer(invocacion -> {
            SolicitudPago pago = invocacion.getArgument(0);
            return new ComprobantePago(pago.pedidoId(), "PAG-TEST", "FAC-TEST", pago.monto(),
                    pago.metodoPago(), Instant.now(), false);
        });
        return clientes;
    }

    @Test
    void integracionDesactivadaNoInvocaCocina() throws Exception {
        ClientesRmi clientes = clientes();
        ServicioPedidos servicio = new ServicioPedidos(clientes, false);
        VistaPedido pedido = servicio.crear("sin-cocina", solicitud).pedido();
        assertEquals(PENDIENTE_INTEGRACION, pedido.estadoPreparacion());
        assertFalse(pedido.pendiente());
        verify(clientes, never()).enviarACocina(any());
        assertThrows(OperacionRechazadaException.class, () -> servicio.marcarListo(pedido.id()));
    }

    @Test
    void cocinaCaidaSeReintentaSinRepetirPagoNiVenta() throws Exception {
        ClientesRmi clientes = clientes();
        doThrow(new RemoteException("Cocina apagada")).doNothing().when(clientes).enviarACocina(any());
        ServicioPedidos servicio = new ServicioPedidos(clientes, true);
        VistaPedido pedido = servicio.crear("cocina-caida", solicitud).pedido();
        assertEquals(PENDIENTE_ENVIO, pedido.estadoPreparacion());
        assertTrue(pedido.pendiente());
        assertTrue(pedido.ventaRegistrada());
        VistaPedido recuperado = servicio.reintentar(pedido.id());
        assertEquals(RECIBIDO, recuperado.estadoPreparacion());
        assertFalse(recuperado.pendiente());
        verify(clientes, times(1)).pagar(any());
        verify(clientes, times(1)).registrarVenta(any());
        verify(clientes, times(2)).enviarACocina(any());
        assertTrue(servicio.marcarListo(pedido.id()));
        assertFalse(servicio.marcarListo(pedido.id()));
        assertEquals(LISTO, servicio.consultar(pedido.id()).estadoPreparacion());
    }

    @Test
    void callbackRmiAntesDeRespuestaNoBloqueaNiPierdeListo() throws Exception {
        ConfiguracionRmi.configurarTimeoutsCliente(2000, 3000);
        ClientesRmi clientes = clientes();
        ServicioPedidos servicio = new ServicioPedidos(clientes, true);
        int puerto;
        try (ServerSocket socket = new ServerSocket(0)) { puerto = socket.getLocalPort(); }
        PedidosRmi entrada = new PedidosRmi(servicio, puerto);
        entrada.iniciar();
        try {
            ServicioEstadoPedidos remoto = (ServicioEstadoPedidos) LocateRegistry.getRegistry("localhost", puerto)
                    .lookup(ServicioEstadoPedidos.NOMBRE);
            doAnswer(invocacion -> {
                PedidoCocina pedido = invocacion.getArgument(0);
                assertTrue(remoto.marcarListo(pedido.pedidoId()));
                return null;
            }).when(clientes).enviarACocina(any());
            VistaPedido pedido = servicio.crear("callback", solicitud).pedido();
            assertEquals(LISTO, pedido.estadoPreparacion());
            assertFalse(remoto.marcarListo(pedido.id()));
            assertThrows(OperacionRechazadaException.class, () -> remoto.marcarListo("no-existe"));
        } finally {
            entrada.cerrar();
        }
    }

    @Test
    void pagoNoConfirmadoNoSeEnviaACocina() throws Exception {
        ClientesRmi clientes = clientes();
        doThrow(new RemoteException("Pago sin respuesta")).when(clientes).pagar(any());
        ServicioPedidos servicio = new ServicioPedidos(clientes, true);
        VistaPedido pedido = servicio.crear("sin-pago", solicitud).pedido();
        verify(clientes, never()).enviarACocina(any());
        assertThrows(OperacionRechazadaException.class, () -> servicio.marcarListo(pedido.id()));
    }

    @Test
    void rechazoCocinaConservaPagoYNoSeReintentaAutomaticamente() throws Exception {
        ClientesRmi clientes = clientes();
        doThrow(new OperacionRechazadaException("Contenido incompatible")).when(clientes).enviarACocina(any());
        ServicioPedidos servicio = new ServicioPedidos(clientes, true);
        VistaPedido pedido = servicio.crear("rechazo", solicitud).pedido();
        assertEquals(RECHAZADO, pedido.estadoPreparacion());
        assertEquals(VistaPedido.EstadoPago.PAGADO, pedido.estadoPago());
        assertNotNull(pedido.comprobante());
        servicio.reintentar(pedido.id());
        verify(clientes, times(1)).enviarACocina(any());
        assertThrows(OperacionRechazadaException.class, () -> servicio.marcarListo(pedido.id()));
    }
}
