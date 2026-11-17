# ADR-002: Java RMI para la comunicación con Pagos, Facturación y Administración

**Estado:** Implementado y probado
**Fecha:** 2026-11-14

## Contexto
Al crear un pedido hay que cobrarlo y facturarlo antes de enviarlo a cocina. Pedidos necesita la respuesta (número de pago y de factura) para decidir el estado del pedido. Todos los servicios están en Java y en la misma red. Un cobro duplicado es inaceptable (escenario E1) y una caída de Pagos no debe tumbar a Pedidos (escenario E2).

## Decisión
Usar **Java RMI síncrono** entre Pedidos y Pagos (`registrarPago`), y entre Pedidos y Administración (`registrarVenta`).

- Contratos tipados en `edu.campusbite.common` (`interface ... extends Remote`).
- Operaciones **idempotentes por `pedidoId`** (semántica *at-most-once*).
- Timeout del cliente con `sun.rmi.transport.tcp.responseTimeout` (3 s) y conexión (2 s).

## Alternativas consideradas
| Alternativa | Por qué no |
|---|---|
| REST interno | Contrato no tipado; habría que serializar y validar JSON a mano entre servicios Java |
| gRPC | Opción moderna y multi-lenguaje, pero exige `.proto` y `protoc`; todo el sistema es Java |
| Kafka para el cobro | El cobro necesita respuesta inmediata (síncrono); la cola agrega infraestructura |
| Sockets TCP | Habría que diseñar un protocolo propio (formato, errores, timeouts) |

## Consecuencias
**Positivas:** transparencia (se llama como un método local), contrato tipado verificado al compilar, excepciones de negocio que viajan al cliente (`OperacionRechazadaException`).

**Negativas:** acoplamiento a Java en ambos extremos. Acoplamiento temporal: si Pagos cae, no se puede cobrar (mitigado con `PENDIENTE_PAGO`). El timeout es ambiguo (la operación pudo completarse), lo que obliga a la idempotencia. Las propiedades `sun.rmi.*` son específicas del JDK.

## Evidencia
Ver `docs/E2_PROCEDIMIENTO.md` y `docs/evidencias/`.