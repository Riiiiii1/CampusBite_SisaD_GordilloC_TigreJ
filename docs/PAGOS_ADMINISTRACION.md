# Pagos, Facturación y Administración

Esta es mi parte del proyecto (John Tigre). Son dos servicios internos en Java, sin Spring, y cada uno corre en su propia JVM con su propio registro RMI. Los contratos compartidos están en `edu.campusbite.common`.

| Servicio | Clase principal | RMI | HTTP |
|---|---|---|---|
| Pagos y Facturación | `edu.campusbite.pagos.PagosMain` | 1101 | 8082 |
| Administración | `edu.campusbite.administracion.AdministracionMain` | 1103 | 8083 |

Los puertos están en `ConfiguracionRmi` (Cocina 1102, Notificaciones 1104, REST de Pedidos 8080, WebSocket 8081).

## Cómo ejecutarlo
```
mvnw.cmd -q compile
java -cp target/classes edu.campusbite.administracion.AdministracionMain
java -cp target/classes edu.campusbite.pagos.PagosMain
```

## Qué hace
- **`registrarPago`** cobra y factura en una sola operación (flecha [2] del flujo). Es idempotente por `pedidoId`: un reintento con el mismo monto devuelve el mismo comprobante con `repetido=true`, y con otro monto se rechaza. Pedidos usa la Idempotency-Key del REST para decidir el `pedidoId`.
- Si el cliente da timeout pero Pagos termina después, el pago queda guardado y `consultarPago` o el reintento lo encuentran.
- **`registrarVenta`** (Administración, flecha [6], la llama Pedidos) es idempotente: la misma venta no se cuenta dos veces.
- Falla simulada: `POST /simulacion/retraso?ms=5000` (0 = normal). Para simular un servicio detenido, se cierra el proceso.

## Consultas REST
- Pagos (8082): `GET /caja/pagos`, `/facturacion/facturas/{pedidoId}`, `/pagos/estado`
- Administración (8083): `GET /admin/estadisticas`, `/admin/ventas`

## Pruebas
Procedimiento y resultados en `docs/E2_PROCEDIMIENTO.md`; la salida de cada ejecución está en `docs/evidencias/`.

## Uso de IA
Use Claude para obtener una propuesta inicial de contratos y servicios, para revisarla y refactorizarla y para las clases de prueba. Los métodos que escribió o ajustó la IA llevan un comentario encima. El detalle de cada consulta y lo que hice con cada respuesta está en `docs/BITACORA_IA_JOHN.md`. Las decisiones de diseño (REST, RMI y WebSocket, reparto y flujo) las tomó el grupo.