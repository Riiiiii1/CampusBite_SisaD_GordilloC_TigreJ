# ADR-004: Elección de REST, RMI y WebSocket

**Estado:** Implementado

**Fecha:** 2026-11-16

## Contexto

La práctica pide REST para la comunicación externa y dos mecanismos más dentro del mismo flujo. Uno de ellos debe permitir comunicación asíncrona.

En CampusBite, el estudiante usa una página web. Pedidos, Pagos, Administración, Cocina y Notificaciones son servicios Java que se ejecutan por separado. El pedido debe cobrarse antes de llegar a Cocina y el estudiante debe recibir un aviso cuando esté listo.

## Decisión

Usar los tres mecanismos de esta manera:

1. **REST:** el navegador crea, consulta y reintenta pedidos mediante la API de Pedidos.
2. **Java RMI:** los servicios Java se llaman entre sí para registrar el pago, la venta, el pedido en Cocina y el cambio de estado a `LISTO`. Cocina también envía por RMI el evento a Notificaciones.
3. **WebSocket:** Notificaciones envía al navegador el aviso de `LISTO` mientras la página está abierta.

Las llamadas REST y RMI esperan una respuesta. El aviso WebSocket llega al navegador de forma asíncrona. Si el estudiante estaba desconectado, la página consulta el estado en Pedidos cuando vuelve a abrirse.

## Por qué se eligieron

REST permite que el navegador cree y consulte pedidos por HTTP, como exige la práctica. RMI permite usar los contratos Java que comparten los servicios y obtener la respuesta necesaria para continuar el pedido. WebSocket permite avisar al navegador cuando Cocina termina, sin que el estudiante tenga que consultar continuamente.

## Otras opciones revisadas

| Opción | Por qué no se usó en este flujo |
|---|---|
| Sockets TCP | Habría que definir el formato de mensajes y el manejo de errores entre servicios. |
| gRPC | Requeriría definir contratos `.proto` y generar código para unos servicios que ya comparten contratos Java. |
| Kafka | Agregaría un servicio más para ejecutar. El cobro necesita una respuesta antes de enviar el pedido a Cocina. |
| Consultar Pedidos periódicamente | Haría consultas aunque el pedido no haya cambiado. WebSocket envía el aviso cuando ocurre el cambio. |

## Consecuencias

Si un servicio RMI no responde, el pedido puede quedar pendiente y se reintenta sin crear otro cobro. Si el navegador pierde el aviso WebSocket, consulta `GET /pedidos/{id}` para recuperar el estado desde Pedidos. Los estados están en memoria y se pierden al reiniciar los procesos.

## Documentación relacionada

- `docs/ARQUITECTURA.md`: contexto, escenarios y mapa de comunicación.
- `docs/ADR-001_REST.md`: creación y consulta de pedidos.
- `docs/ADR-002_RMI.md`: comunicación con Pagos y Administración.
- `docs/ADR-003_COCINA_WEBSOCKET.md`: Cocina y avisos al estudiante.
- `docs/E2_PROCEDIMIENTO.md` y `docs/E3_E4_PROCEDIMIENTO.md`: pruebas realizadas.