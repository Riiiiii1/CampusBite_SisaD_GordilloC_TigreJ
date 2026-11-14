package edu.campusbite.administracion;

import edu.campusbite.common.dto.EstadisticasVentas;
import edu.campusbite.common.dto.Venta;
import edu.campusbite.common.rmi.ServicioAdministracion;
import edu.campusbite.common.util.Registro;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

//Guarda las ventas en memoria y calcula estadisticas
public class ServicioAdministracionImpl implements ServicioAdministracion {

    // Una venta por pedido: un reintento de Pedidos no la cuenta dos veces
    private final ConcurrentHashMap<String, Venta> ventasPorPedido = new ConcurrentHashMap<>();

    // Metodo ajustado con ayuda de IA (Claude): usa el log compartido; la venta la envia Pedidos (flecha [6]).
    @Override
    // ------------------------------- MODIFICADO DAVID - ORIGINAL JONH -------------------------------
    public boolean registrarVenta(Venta venta) {
        if (venta == null || venta.pedidoId() == null || venta.pedidoId().isBlank()
                || venta.monto() == null || venta.monto().signum() <= 0 || venta.fecha() == null) {
            throw new IllegalArgumentException("Venta invalida: requiere pedidoId, monto positivo y fecha");
        }
        Venta anterior = ventasPorPedido.putIfAbsent(venta.pedidoId(), venta);
        if (anterior != null && anterior.monto().compareTo(venta.monto()) != 0) {
            throw new IllegalArgumentException("El pedido ya tiene una venta con otro monto");
        }
        boolean nueva = anterior == null;
        Registro.info("ADMIN", (nueva ? "VENTA registrada " : "VENTA repetida ignorada ")
                + venta.pedidoId() + " por $" + venta.monto());
        return nueva;
    }

    @Override
    // ------------------------------- MODIFICADO DAVID - ORIGINAL JONH -------------------------------
    public EstadisticasVentas obtenerEstadisticas() {
        List<Venta> ventas = listarVentas();
        long cantidad = ventas.size();
        BigDecimal total = ventas.stream()
                .map(Venta::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal promedio = cantidad == 0
                ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
        return new EstadisticasVentas(cantidad, total, promedio);
    }

    public List<Venta> listarVentas() {
        return new ArrayList<>(ventasPorPedido.values());
    }
}
