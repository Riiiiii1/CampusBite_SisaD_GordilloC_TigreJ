package edu.campusbite.notificaciones;

import edu.campusbite.common.dto.EventoPedidoListo;
import edu.campusbite.common.util.Registro;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static edu.campusbite.common.http.RespuestaHttp.texto;

/**
 * Envia avisos en vivo a los navegadores suscritos a un pedido
 * El navegador se conecta a /ws?pedidoId={id}
 */
@Component
public class NotificacionesWebSocketHandler extends TextWebSocketHandler {

    private static final String SERVICIO = "NOTIFICACIONES";

    private final ConcurrentHashMap<String, Set<WebSocketSession>> sesionesPorPedido =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, String> pedidoPorSesion =
            new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession sesion)
            throws IOException {
        String pedidoId = obtenerPedidoId(sesion);

        if (pedidoId == null || pedidoId.isBlank()) {
            sesion.close(CloseStatus.BAD_DATA);
            return;
        }

        sesionesPorPedido
                .computeIfAbsent(pedidoId, id -> ConcurrentHashMap.newKeySet())
                .add(sesion);

        pedidoPorSesion.put(sesion.getId(), pedidoId);

        Registro.info(SERVICIO,
                "Cliente WebSocket conectado al pedido " + pedidoId);

        // Al reconectar, el navegador consulta GET /pedidos/{id}
        // Pedidos sigue siendo la fuente de verdad del estado
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession sesion,
            CloseStatus estado) {
        quitarSesion(sesion);
    }

    @Override
    public void handleTransportError(
            WebSocketSession sesion,
            Throwable error) {
        quitarSesion(sesion);
        try {
            if (sesion.isOpen()) {
                sesion.close(CloseStatus.SERVER_ERROR);
            }
        } catch (IOException ignorada) {
            // La conexión ya no está disponible
        }
    }

    /**
     * Envia un evento a los clientes conectados a ese pedido
     * Revisé este envío con ayuda de ChatGPT
     * @return cantidad de sesiones que recibieron el mensaje
     */
    public int enviar(EventoPedidoListo evento) {
        Set<WebSocketSession> sesiones =
                sesionesPorPedido.get(evento.pedidoId());

        if (sesiones == null || sesiones.isEmpty()) {
            return 0;
        }

        String contenido = aJson(evento);
        int enviadas = 0;

        for (WebSocketSession sesion : sesiones) {
            try {
                synchronized (sesion) {
                    if (sesion.isOpen()) {
                        sesion.sendMessage(new TextMessage(contenido));
                        enviadas++;
                    } else {
                        quitarSesion(sesion);
                    }
                }
            } catch (IOException e) {
                quitarSesion(sesion);
                Registro.info(SERVICIO,
                        "No se pudo enviar WebSocket para el pedido "
                                + evento.pedidoId());
            }
        }

        return enviadas;
    }

    private static String obtenerPedidoId(WebSocketSession sesion) {
        if (sesion.getUri() == null
                || sesion.getUri().getRawQuery() == null) {
            return null;
        }

        String consulta = sesion.getUri().getRawQuery();

        for (String parametro : consulta.split("&")) {
            String[] partes = parametro.split("=", 2);

            if (partes.length == 2 && "pedidoId".equals(partes[0])) {
                return URLDecoder.decode(
                        partes[1],
                        StandardCharsets.UTF_8);
            }
        }

        return null;
    }

    private void quitarSesion(WebSocketSession sesion) {
        String pedidoId = pedidoPorSesion.remove(sesion.getId());

        if (pedidoId == null) {
            return;
        }

        Set<WebSocketSession> sesiones =
                sesionesPorPedido.get(pedidoId);

        if (sesiones != null) {
            sesiones.remove(sesion);
            if (sesiones.isEmpty()) {
                sesionesPorPedido.remove(pedidoId, sesiones);
            }
        }

        Registro.info(SERVICIO,
                "Cliente WebSocket desconectado del pedido " + pedidoId);
    }

    private static String aJson(EventoPedidoListo evento) {
        return "{\"eventoId\":" + texto(evento.eventoId())
                + ",\"pedidoId\":" + texto(evento.pedidoId())
                + ",\"estudianteId\":" + texto(evento.estudianteId())
                + ",\"estado\":\"LISTO\""
                + ",\"fecha\":" + texto(evento.fecha().toString())
                + "}";
    }
}
