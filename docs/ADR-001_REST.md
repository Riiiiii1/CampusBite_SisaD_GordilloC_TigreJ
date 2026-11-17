# ADR-001: REST para crear y consultar pedidos

**Estado:** Implementado

**Fecha:** 2026-11-16

## Contexto

El estudiante crea su pedido desde la página web y necesita consultar qué pasó con él. También puede volver a la página después de perder la conexión. Pedidos conserva el estado del pedido mientras el servicio está ejecutándose.

La práctica pide usar REST para la comunicación externa. En CampusBite, el navegador se comunica por HTTP con Pedidos en el puerto 8080.

## Decisión

Usar REST para estas operaciones de Pedidos:

- `POST /pedidos`: crea un pedido.
- `GET /pedidos/{id}`: consulta su estado.
- `POST /pedidos/{id}/reintentar`: vuelve a intentar el procesamiento del mismo pedido cuando quedó pendiente.

Al crear un pedido, el navegador envía la cabecera `Idempotency-Key`. Si repite la solicitud con la misma clave y los mismos datos, Pedidos conserva el mismo pedido. Si usa esa clave con datos distintos, Pedidos rechaza la solicitud.

La página consulta `GET /pedidos/{id}` al volver a abrirse o al recuperar la conexión. Así muestra el estado que tiene Pedidos aunque el navegador no haya recibido el aviso WebSocket.

## Otras opciones

| Opción | Por qué no se usa para crear y consultar pedidos |
|---|---|
| Java RMI | El navegador no llama directamente a los servicios Java por RMI. En CampusBite, RMI comunica los servicios entre sí. |
| WebSocket | En este proyecto se usa para enviar el aviso de `LISTO`. La creación y la consulta del pedido tienen direcciones REST y una respuesta HTTP. |

## Consecuencias

**A favor:** el navegador puede crear y consultar pedidos, y recuperar el estado después de una desconexión. La clave de idempotencia permite repetir una solicitud sin crear otro pedido.

**Límite:** los pedidos se guardan en memoria. Si se reinicia Pedidos, se pierde el estado que tenía ese proceso.

## Evidencia

La prueba REST se ejecutó el 16/11/2026 con `BUILD SUCCESS`. `IntegracionPedidosTest` terminó con 6 pruebas, 0 fallos y 0 errores. Ver [procedimiento REST](REST_PROCEDIMIENTO.md) y [reporte generado por Maven](evidencias/REST_integracion_pedidos.txt). Las capturas del funcionamiento están en `assets/`.