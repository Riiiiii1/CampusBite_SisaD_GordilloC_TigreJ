# Bitácora de IA – Carlos Gordillo

**Herramienta:** ChatGPT (OpenAI), utilizada durante el desarrollo de CampusBite en los módulos de Cocina, Notificaciones, WebSocket, página HTML e integración con Pedidos.

| # | Qué le pedí | Qué me respondió | Qué hice yo / el grupo |
|---|---|---|---|
| 1 | Analizar el trabajo de David y John antes de iniciar mi parte. | Explicó la conexión entre Pedidos, Pagos, Cocina y Notificaciones, y propuso un plan de implementación. | Confirmé con David los contratos RMI, puertos y el orden de comunicación entre servicios. |
| 2 | Guiarme en la implementación de Cocina archivo por archivo. | Propuso el servicio RMI, almacenamiento de pedidos, control de duplicados y API HTTP. | Creé los archivos en IDEA, revisé el código y probé el flujo con pedidos reales. |
| 3 | Ayudarme con Notificaciones y la página del estudiante. | Propuso el servicio RMI, WebSocket y un formulario HTML para crear y consultar pedidos. | Integré los servicios y comprobé la recepción de pedidos pagados y la actualización de su estado. |
| 4 | Explicar errores durante el inicio y las pruebas. | Indicó cómo configurar `JAVA_HOME`, compilar con Maven y ejecutar los cinco procesos. | Configuré Java 17, obtuve `BUILD SUCCESS` y ejecuté todos los servicios. |
| 5 | Corregir los botones y la tarjeta de estado. | Propuso varias modificaciones en JavaScript, algunas con cambios no solicitados. | Rechacé esos cambios, expliqué el comportamiento esperado y revisé las modificaciones antes de aplicarlas. |
| 6 | Integrar mis cambios con los de David. | Explicó el conflicto de `index.html` durante el merge y cómo combinar ambas implementaciones. | Resolví el conflicto en IDEA, compilé, creé el commit y subí mi rama. |
| 7 | Guiarme en las pruebas de reintentos y notificaciones. | Indicó qué servicio detener, qué pedido crear y cuándo tomar capturas. | Comprobé que los pedidos quedaran `PENDIENTE_ENVIO` y cambiaran a `RECIBIDO` al reintentar, sin duplicarse. |
| 8 | Guiarme en las pruebas E3 y E4. | Propuso utilizar dos ventanas y desconectar una antes de marcar otro pedido como listo. | Comprobé la actualización automática a `LISTO` en E3 y, en E4, verifiqué que el estado se conservara al salir de la página y volver a abrirla. |

## Código con ayuda de IA

Utilicé la IA como apoyo para desarrollar y revisar Cocina, Notificaciones, WebSocket y `src/main/resources/static/index.html`. Sin embargo, adapté el código en IDEA, respeté los acuerdos con David y realicé pruebas manuales antes de validar los cambios.

Estos son algunos de los métodos y funciones que trabajé o revisé con ayuda de IA:

- En Cocina: `ServicioCocinaImpl.registrarPedido`, `ServicioCocinaImpl.marcarListo` y `ApiCocina.marcarListo`
- En Notificaciones: `ServicioNotificacionesImpl.notificarListo` y `NotificacionesWebSocketHandler.enviar`
- En la página HTML: `actualizarAcciones`, `consultarEstado` y `conectarWebSocket`

## Reflexión

- **Dónde la IA no aplicaba o se equivocó:** En algunas propuestas modificó el HTML y agregó mensajes que no había solicitado. Tuve que aclarar el comportamiento esperado y revisar los reemplazos antes de aplicarlos.
- **Dónde se quedó corta:** Una compilación exitosa no garantiza que JavaScript ni la comunicación entre los cinco servicios funcionen correctamente. Por eso, realicé pruebas con pedidos reales, reintentos y dos ventanas.
- **Qué aprendí:** Comprendí que Pedidos debe conservar el estado `LISTO` aunque se pierda la notificación WebSocket. Por eso, al regresar, la página consulta `GET /pedidos/{id}`. También aprendí a verificar los reintentos con el mismo `pedidoId` y comprobar que no existan registros duplicados en Cocina ni en Caja.
