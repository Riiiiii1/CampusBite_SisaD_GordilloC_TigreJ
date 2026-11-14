package edu.campusbite.pagos.demo;

import edu.campusbite.common.dto.ComprobantePago;
import edu.campusbite.common.dto.SolicitudPago;
import edu.campusbite.common.rmi.ConfiguracionRmi;
import edu.campusbite.common.rmi.ServicioPagos;
import java.math.BigDecimal;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/*
    Prueba E1 (solicitud duplicada): 20 llamadas simultaneas para el MISMO pedido.
    Debe existir un solo pago y una sola factura. Necesita PagosMain corriendo.
*/
public class PruebaDuplicados {

    // Metodo escrito con ayuda de IA (Claude): lanza 20 llamadas a la vez con el mismo pedido y cuenta los pagos distintos.
    public static void main(String[] args) throws Exception {
        ServicioPagos pagos = (ServicioPagos) LocateRegistry
                .getRegistry(ConfiguracionRmi.HOST, ConfiguracionRmi.PUERTO_PAGOS)
                .lookup(ServicioPagos.NOMBRE);

        String pedido = "DUP-" + System.nanoTime();
        int llamadas = 20;
        ExecutorService pool = Executors.newFixedThreadPool(llamadas);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<ComprobantePago>> futuros = new ArrayList<>();
        for (int i = 0; i < llamadas; i++) {
            futuros.add(pool.submit(() -> {
                salida.await();
                return pagos.registrarPago(new SolicitudPago(pedido, new BigDecimal("4.50"), "EFECTIVO"));
            }));
        }
        salida.countDown();

        Set<String> pagosDistintos = new HashSet<>();
        Set<String> facturasDistintas = new HashSet<>();
        int repetidos = 0;
        for (Future<ComprobantePago> f : futuros) {
            ComprobantePago c = f.get();
            pagosDistintos.add(c.pagoId());
            facturasDistintas.add(c.numeroFactura());
            if (c.repetido()) {
                repetidos++;
            }
        }
        System.out.println("Llamadas simultaneas        : " + llamadas);
        System.out.println("Pagos distintos creados     : " + pagosDistintos.size() + "  (esperado: 1)");
        System.out.println("Facturas distintas emitidas : " + facturasDistintas.size() + "  (esperado: 1)");
        System.out.println("Respuestas marcadas repetido: " + repetidos + "  (esperado: 19)");
        boolean ok = pagosDistintos.size() == 1 && facturasDistintas.size() == 1 && repetidos == llamadas - 1;
        System.out.println("Resultado E1                : " + (ok ? "OK" : "FALLO"));
        pool.shutdownNow();
    }
}