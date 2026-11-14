package edu.campusbite.pagos.demo;

import edu.campusbite.common.dto.ComprobantePago;
import edu.campusbite.common.dto.SolicitudPago;
import edu.campusbite.common.rmi.ConfiguracionRmi;
import edu.campusbite.common.rmi.OperacionRechazadaException;
import edu.campusbite.common.rmi.ServicioPagos;

import java.math.BigDecimal;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

/*
    Cliente de prueba para ejecutar E2 sin depender de Pedidos.
    Hace lo mismo que hara Pedidos: llamar a registrarPago con un limite de espera.
    Uso:
        registrar P-001 3.50 [timeoutMs]
        consultar P-001
 */
public class ClienteDemoPagos {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Uso: registrar <pedidoId> <monto> [timeoutMs] | consultar <pedidoId>");
            return;
        }
        long timeoutMs = args.length >= 4 ? Long.parseLong(args[3]) : 3000;
        // Debe configurarse ANTES del primer lookup.
        ConfiguracionRmi.configurarTimeoutsCliente(2000, timeoutMs);

        ServicioPagos pagos;
        try {
            pagos = (ServicioPagos) LocateRegistry
                    .getRegistry(ConfiguracionRmi.HOST, ConfiguracionRmi.PUERTO_PAGOS)
                    .lookup(ServicioPagos.NOMBRE);
        } catch (RemoteException | NotBoundException e) {
            System.out.println("PAGOS NO DISPONIBLE -> " + e.getClass().getSimpleName()
                    + ": el cliente debe responder de forma controlada (p. ej. PENDIENTE_PAGO)");
            return;
        }

        String pedidoId = args[1];
        if ("consultar".equals(args[0])) {
            ComprobantePago c = pagos.consultarPago(pedidoId);
            System.out.println(c == null ? "Sin pago registrado para " + pedidoId : "Pago registrado: " + c);
            return;
        }

        long inicio = System.currentTimeMillis();
        try {
            ComprobantePago c = pagos.registrarPago(new SolicitudPago(pedidoId, new BigDecimal(args[2]), "EFECTIVO"));
            System.out.println("OK en " + (System.currentTimeMillis() - inicio) + " ms -> " + c);
        } catch (OperacionRechazadaException e) {
            System.out.println("RECHAZADO en " + (System.currentTimeMillis() - inicio) + " ms -> " + e.getMessage());
        } catch (RemoteException e) {
            System.out.println("SIN RESPUESTA tras " + (System.currentTimeMillis() - inicio) + " ms -> "
                    + e.getClass().getSimpleName() + " (causa: "
                    + (e.getCause() == null ? "-" : e.getCause().getClass().getSimpleName()) + ")");
        }
    }
}