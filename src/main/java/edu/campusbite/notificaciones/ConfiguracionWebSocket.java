package edu.campusbite.notificaciones;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class ConfiguracionWebSocket implements WebSocketConfigurer {

    private final NotificacionesWebSocketHandler manejador;

    public ConfiguracionWebSocket(
            NotificacionesWebSocketHandler manejador) {
        this.manejador = manejador;
    }

    @Override
    public void registerWebSocketHandlers(
            WebSocketHandlerRegistry registro) {
        registro.addHandler(manejador, "/ws")
                .setAllowedOriginPatterns("*");
    }
}