package edu.campusbite.common.http;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Utilidades minimas para responder JSON con el servidor HTTP del JDK. */
public final class RespuestaHttp {

    private RespuestaHttp() {
    }

    public static void json(HttpExchange ex, int estado, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(estado, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void error(HttpExchange ex, int estado, String mensaje) throws IOException {
        json(ex, estado, "{\"error\":" + texto(mensaje) + "}");
    }

    /** Convierte un texto en una cadena JSON (con comillas y escapes basicos). */
    public static String texto(Object valor) {
        if (valor == null) {
            return "null";
        }
        String s = valor.toString().replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + s + "\"";
    }
}
