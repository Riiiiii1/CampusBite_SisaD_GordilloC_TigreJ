# Prueba REST: solicitud duplicada

## Objetivo

Comprobar que repetir la creación de un pedido no genere otro pedido, pago o venta. Esta prueba corresponde al escenario de prioridad 1 de `docs/ARQUITECTURA.md`.

## Cómo se realizó

Se detuvieron los cinco servicios que estaban abiertos en IntelliJ y se ejecutó `.\mvnw.cmd test` con JDK 17. La prueba `duplicadosConcurrentesYValidacionRest`, dentro de `IntegracionPedidosTest`, envía 20 solicitudes `POST /pedidos` con la misma `Idempotency-Key` y los mismos datos.

La prueba también repite la solicitud, cambia los datos usando la misma clave y envía solicitudes inválidas. Cocina está desactivada solo dentro de esta clase de pruebas para comprobar REST, Pagos y Administración sin depender de que Cocina esté ejecutándose.

## Resultado obtenido

El 16/11/2026 Maven terminó con `BUILD SUCCESS`. `IntegracionPedidosTest` ejecutó 6 pruebas, con 0 fallos y 0 errores.

La prueba de solicitudes duplicadas verificó que las 20 respuestas corresponden a un solo pedido, un solo pago y una sola venta. Al repetir la solicitud con los mismos datos, Pedidos respondió `200`. Con la misma clave y otros datos respondió `409`. También se comprobaron las respuestas `400` para datos inválidos o sin clave y `404` para un pedido inexistente.

## Interpretación

La cabecera `Idempotency-Key` permite reconocer la solicitud repetida. Así, un reintento del navegador no crea otro pedido ni provoca otro cobro o registro de venta.

## Evidencia

El reporte generado por Maven está en `docs/evidencias/REST_integracion_pedidos.txt`. El código de la prueba está en `src/test/java/edu/campusbite/pedidos/IntegracionPedidosTest.java`.