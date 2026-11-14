# Bitácora de IA - John Tigre

Herramienta: Claude (Anthropic), usada del 10 al 14/11/2026. Esta bitácora cubre mi parte del proyecto: Pagos, Facturación y Administración.

| # | Qué le pedí | Qué me respondió | Qué hice yo / el grupo |
|---|---|---|---|
| 1 | Que me ayudara a entender el enunciado| Resumió lo que había que entregar y sugirió REST + Kafka + WebSocket | El grupo no usó Kafka: elegimos **REST + Java RMI + WebSocket** |
| 2 | Cómo encajar esos tres mecanismos en un solo flujo y qué fallas probar | Propuso un flujo de seis pasos y tres pruebas de falla (duplicado, servicio que no responde, cliente que se desconecta) | Lo llevé al grupo y nos repartimos el trabajo: David hace Pedidos, Carlos Cocina y Notificaciones, y yo Pagos, Facturación y Administración |
| 3 | Que revisara el diagrama de contexto y el mapa de microservicios | Marcó errores del mapa (una flecha del WebSocket mal puesta, una flecha sin sentido y Administración sin conexión) | Corregimos el mapa con esas observaciones |
| 4 | Dudas del flujo: qué pasa con el timeout de RMI y cómo recupera el estado un cliente que se reconecta | Explicó que el timeout no cancela la operación y propuso que Notificaciones guarde el último estado para reenviarlo | Ajustamos el flujo: Cocina avisa a Pedidos cuando el pedido está listo, y Notificaciones guarda el último estado |
| 5 | Escribí el código de mi parte (Pagos y Administración) y le pedí que lo revisara y lo corrigiera | Devolvió una versión corregida, con un `LEEME`. Después me dijo que no encajaba con el plan: Pagos enviaba la venta a Administración (el plan dice que la envía Pedidos), los puertos chocaban con el WebSocket y el log estaba duplicado. También señaló un error en `/simulacion/retraso` | Decidí alinear mi parte con el plan y rehacerla en una rama limpia, archivo por archivo |
| 6 | Que me guiara paso a paso con el código corregido | Me dio cada archivo y los comandos para compilar, probar y subir | Escribí cada archivo, compilé en cada paso y subí un commit por paso. Cambié algunos comentarios. Un archivo se subió vacío porque no lo guardé antes del commit y lo corregí en el siguiente |
| 7 | Cómo arreglar los errores que me iban saliendo | Explicó las causas: el archivo vacío, los servicios que no estaban corriendo y los archivos en UTF-16 que git tomaba como binarios | Los arreglé yo. Repetí la prueba de Pagos apagado porque Pagos seguía corriendo y la prueba no era válida |
| 8 | Que hiciera clases de prueba para duplicados y para Administración | Escribió `PruebaDuplicados` y `ClienteDemoAdministracion` | Las ejecuté con los servicios corriendo y guardé la salida en `docs/evidencias/` |


## Código con ayuda de IA
Los métodos que la IA escribió o ajustó llevan un comentario encima que lo dice:
- `ServicioPagosImpl.registrarPago`
- `ApiConsultasPagos.cambiarRetraso`
- `ServicioAdministracionImpl.registrarVenta`
- `Registro.info`
- `PruebaDuplicados.main`
- `ClienteDemoAdministracion.main`

El resto del código lo escribí yo y la IA lo revisó y lo corrigió (fila 5). La versión que subí al repositorio parte de esa versión corregida, que revisé, adapté, compilé y probé. No afirmo haberla escrito desde cero.

## Reflexión
- **Dónde la IA no aplicaba:** propuso Kafka, pero nuestro cobro necesita una respuesta inmediata y Kafka agrega una infraestructura que no hacía falta, por eso el grupo eligió RMI. También entregó una versión que movía el envío de la venta a Pagos, con otros puertos y otra estructura de módulos, y eso no encajaba con el plan ni con el repositorio de David.
- **Dónde se quedó corta:** en el ADR-002 dio por suficiente `Future.get(timeout)`, pero eso solo libera al que espera y no cancela la llamada. Lo corregimos con el timeout del socket y con la idempotencia por `pedidoId`.
- **Qué aprendí:** que un timeout no dice si la operación se hizo o no. Con Pagos lento, mi cliente dio timeout a los 3 segundos pero el pago se completó igual. Por eso `registrarPago` es idempotente: con `computeIfAbsent`, 20 llamadas iguales crearon un solo pago y una sola factura. También aprendí que una prueba solo vale si el estado es el correcto y que hay que guardar y revisar cada archivo antes del commit.