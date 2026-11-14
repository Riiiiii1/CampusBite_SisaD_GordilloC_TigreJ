package edu.campusbite.administracion;

import edu.campusbite.common.dto.EstadisticasVentas;
import edu.campusbite.common.dto.Venta;
import edu.campusbite.common.rmi.ConfiguracionRmi;
import edu.campusbite.common.rmi.ServicioAdministracion;

import java.math.BigDecimal;
import java.rmi.registry.LocateRegistry;
import java.time.Instant;

public class ClienteDemoAdministracion {

    // Metodo escrito con ayuda de IA (Claude): envia la misma venta dos veces y muestra las estadisticas.
    public static void main(String[] args) throws Exception {
        ServicioAdministracion admin = (ServicioAdministracion) LocateRegistry
                .getRegistry(ConfiguracionRmi.HOST, ConfiguracionRmi.PUERTO_ADMINISTRACION)
                .lookup(ServicioAdministracion.NOMBRE);
        String pedido = "VENTA-" + System.nanoTime();
        Venta venta = new Venta(pedido, new BigDecimal("5.00"), Instant.now());

        System.out.println("Primer envio  -> nueva = " + admin.registrarVenta(venta) + "  (esperado: true)");
        System.out.println("Segundo envio -> nueva = " + admin.registrarVenta(venta) + "  (esperado: false)");
        EstadisticasVentas e = admin.obtenerEstadisticas();
        System.out.println("Estadisticas: " + e);
    }
}