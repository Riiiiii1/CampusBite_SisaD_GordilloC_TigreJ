# ADR-003: RMI para Cocina y WebSocket para notificaciones

**Estado:** Implementado y probado

**Fecha:** 2026-11-15

## Contexto

Pedidos envía las órdenes a Cocina después de confirmar el pago. Cuando termina la preparación, el estudiante debe recibir una notificación si tiene la página abierta y poder consultar el estado si se desconectó.

Los servicios están desarrollados en Java. Pedidos debe mantener el estado oficial de cada pedido, evitando que una notificación perdida lo deje permanentemente en `RECIBIDO`.

## Decisión

- Pedidos se comunica con Cocina mediante Java RMI, usando `ServicioCocina.registrarPedido(PedidoCocina)` en el registro 1102. Cocina almacena los pedidos en memoria y evita duplicados mediante `pedidoId`.
- Cocina dispone de una API HTTP en el puerto 8084 para consultar pedidos y marcarlos como listos.
- Al finalizar la preparación, Cocina actualiza primero Pedidos mediante RMI con `ServicioEstadoPedidos.marcarListo(pedidoId)` en el registro 1105.
- Después, envía la notificación mediante `ServicioNotificaciones.notificarListo(EventoPedidoListo)` en el registro 1104.
- Notificaciones envía el aviso al navegador mediante WebSocket en el puerto 8081.
- Al abrir o recuperar la página, el navegador consulta `GET /pedidos/{id}` en el puerto 8080 para obtener el estado actual, incluso si perdió la notificación WebSocket.

## Alternativas consideradas

| Alternativa | Motivo para no elegirla |
|---|---|
| Consultar periódicamente a Pedidos | Generaría consultas repetidas aunque no existan cambios y no ofrecería notificaciones inmediatas. |
| Guardar solo el último aviso en Notificaciones | La información está en memoria y se pierde al reiniciar el servicio. Pedidos ya conserva el estado vigente mientras siga ejecutándose. |
| Notificar antes de actualizar Pedidos | El estudiante podría recibir `LISTO` mientras `GET /pedidos/{id}` todavía muestra `RECIBIDO`. |

## Consecuencias

**Positivas:** las notificaciones llegan en tiempo real a los navegadores conectados, Pedidos actualiza primero el estado y los estudiantes pueden recuperarlo al reconectarse.

**Negativas:** los pedidos y estados se almacenan en memoria, por lo que se pierden al reiniciar los procesos. Si Notificaciones está apagado, no se envían avisos en tiempo real, aunque Pedidos puede conservar el estado `LISTO`.

## Evidencia

Consultar `docs/E3_E4_PROCEDIMIENTO.md` y las capturas y resultados de las pruebas E3/E4 en `docs/evidencias/`.