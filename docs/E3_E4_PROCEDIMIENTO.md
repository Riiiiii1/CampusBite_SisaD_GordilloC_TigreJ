# Pruebas E3 y E4: notificación y desconexión del estudiante

Las pruebas se realizaron con los cinco servicios ejecutándose por separado: Pagos, Administración, Pedidos, Cocina y Notificaciones.

## E3 - Aviso en tiempo real

**Objetivo:** comprobar que el estudiante recibe el cambio de estado a LISTO sin consultar manualmente el pedido.

1. Se creó un pedido pagado que Cocina recibió como RECIBIDO.
2. Se abrió http://localhost:8081 en dos ventanas con el mismo pedido.
3. Desde la ventana izquierda se pulsó «Marcar como listo (Cocina)».
4. La ventana derecha permaneció abierta sin realizar ninguna acción.

**Resultado:** ambas ventanas mostraron Preparación: LISTO. La segunda se actualizó automáticamente, sin intervención del estudiante. Se utilizó el pedido 2b51d834-95e0-49b4-a33a-e8af6f5b7b8d.

## E4 - Estudiante desconectado

**Objetivo:** comprobar que el estado del pedido se recupera aunque el estudiante no reciba la notificación en tiempo real.

1. Se creó el pedido e7f55dbb-c7df-405e-bc8c-8f796615960f. Su pago quedó como PAGADO y Cocina lo recibió como RECIBIDO.
2. Se abrió about:blank en la ventana del estudiante, cerrando CampusBite y su conexión WebSocket. Los cinco servicios continuaron ejecutándose.
3. Mientras el estudiante estaba desconectado, se marcó el pedido como listo desde PowerShell:

   ```powershell
   Invoke-RestMethod -Method Post -Uri "http://localhost:8084/cocina/pedidos/e7f55dbb-c7df-405e-bc8c-8f796615960f/listo"
   ```

4. Se consultó el estado directamente desde Pedidos:

   ```powershell
   Invoke-RestMethod -Uri "http://localhost:8080/pedidos/e7f55dbb-c7df-405e-bc8c-8f796615960f"
   ```

5. Se volvió a abrir `http://localhost:8081` en la ventana del estudiante, sin pulsar ningún botón.

**Resultado:** Cocina respondió LISTO y estadoCambio = True. Pedidos devolvió estadoPreparacion : LISTO. Al regresar, la página mostró Preparación: LISTO sin pulsar «Consultar estado». Además, Cocina mostró el pedido una sola vez y Caja registró un solo cobro.

## Interpretación

Pedidos conserva el estado actualizado de cada pedido. WebSocket permite recibir notificaciones en tiempo real mientras la página está abierta. Si el estudiante se desconecta, al regresar la página consulta GET /pedidos/{id} para recuperar el estado directamente desde Pedidos.