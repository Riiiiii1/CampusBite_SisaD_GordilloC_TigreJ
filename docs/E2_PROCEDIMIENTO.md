# Pruebas de falla de Pagos y Administración (E1 y E2)

Escenarios: **E1** solicitud duplicada y **E2** un servicio interno no responde. Cada salida está guardada en `docs/evidencias/`. Se ejecutó con Pagos y Administración corriendo cada uno en su terminal.

## E1 - Solicitud duplicada
- `java -cp target\classes edu.campusbite.pagos.demo.PruebaDuplicados`
- `java -cp target\classes edu.campusbite.administracion.ClienteDemoAdministracion`

## E2 - Pagos lento y cliente con timeout
```powershell
curl.exe -X POST "http://localhost:8082/simulacion/retraso?ms=5000"
java -cp target\classes edu.campusbite.pagos.demo.ClienteDemoPagos registrar P-002 4.00
java -cp target\classes edu.campusbite.pagos.demo.ClienteDemoPagos consultar P-002
curl.exe -X POST "http://localhost:8082/simulacion/retraso?ms=0"
java -cp target\classes edu.campusbite.pagos.demo.ClienteDemoPagos registrar P-002 4.00
java -cp target\classes edu.campusbite.pagos.demo.ClienteDemoPagos registrar P-002 9.00
```

## E2 - Pagos apagado
Se detuvo el proceso de Pagos y se ejecutó `... ClienteDemoPagos registrar P-005 2.00`.

## Resultados obtenidos (14/11/2026)
| Prueba | Esperado | Obtenido | Evidencia |
|---|---|---|---|
| E1: 20 llamadas iguales | 1 pago, 1 factura, 19 repetidas | 1 pago, 1 factura, 19 repetidas, `Resultado E1: OK` | `E1_pagos_duplicados.txt` |
| E1: venta repetida en Administración | `true` y luego `false` | `nueva = true`, luego `nueva = false`; 1 venta en las estadísticas | `E1_venta_repetida.txt` |
| E2: Pagos lento 5 s, timeout 3 s | `SocketTimeoutException` a ~3000 ms | `UnmarshalException` (causa `SocketTimeoutException`) a los 3012 ms | `E2_1_timeout.txt` |
| `consultar` tras el timeout | El pago existe | `PAG-2` registrado para `P-002`: el cobro terminó después del timeout | `E2_2_consulta.txt` |
| Reintento mismo pedido y monto | Mismo `pagoId`, `repetido=true` | `PAG-2`, `repetido=true`, en 23 ms | `E2_3_reintento.txt` |
| Mismo pedido, otro monto | Rechazado | `RECHAZADO ... ya tiene un pago con un monto distinto (4.00)` | `E2_4_otro_monto.txt` |
| Pagos apagado | `ConnectException` | `PAGOS NO DISPONIBLE -> ConnectException` | `E2_5_pagos_apagado.txt` |

## Explicación (para la defensa)
El timeout del cliente **no cancela** la operación remota: solo deja de esperar. Desde el cliente no se puede distinguir "Pagos no lo hizo" de "lo hizo pero la respuesta no llegó". Por eso `registrarPago` es idempotente por `pedidoId` (`computeIfAbsent` es atómico): el cobro ocurre a lo sumo una vez aunque el cliente reintente.
`sun.rmi.transport.tcp.responseTimeout` es un timeout del socket, así que además libera el hilo del cliente (`Future.get` solo deja de esperar).