package cl.duoc.skyrimemporium.servicio;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.skyrimemporium.dto.PedidoResponse;
import cl.duoc.skyrimemporium.dto.ReporteAdminResponse;
import cl.duoc.skyrimemporium.dto.ReporteVentaProducto;
import cl.duoc.skyrimemporium.repositorio.PedidoRepository;
import cl.duoc.skyrimemporium.repositorio.ProductoRepository;

@Service
public class ReporteAdminService {

    private final PedidoRepository pedidos;
    private final ProductoRepository productos;

    public ReporteAdminService(PedidoRepository pedidos, ProductoRepository productos) {
        this.pedidos = pedidos;
        this.productos = productos;
    }

    @Transactional(readOnly = true)
    public ReporteAdminResponse obtener() {
        var ordenes = pedidos.findAllByOrderByFechaDesc();
        Map<String, Acumulado> ventas = new LinkedHashMap<>();
        long ingresos = 0;
        for (var pedido : ordenes) {
            ingresos = Math.addExact(ingresos, pedido.getTotal());
            pedido.getItems().forEach(item -> {
                Acumulado venta = ventas.computeIfAbsent(item.getNombre(), Acumulado::new);
                venta.unidades += item.getCantidad();
                venta.ingresos += item.getTotal();
            });
        }
        List<ReporteVentaProducto> masVendidos = ventas.values().stream()
                .sorted(Comparator.comparingLong((Acumulado v) -> v.unidades).reversed())
                .limit(10)
                .map(v -> new ReporteVentaProducto(v.nombre, v.unidades, v.ingresos))
                .toList();
        long criticos = productos.findAll().stream()
                .filter(p -> p.getStock() <= p.getStockCritico()).count();
        return new ReporteAdminResponse(ingresos, ordenes.size(), criticos, masVendidos,
                ordenes.stream().limit(5).map(PedidoResponse::desde).toList());
    }

    private static class Acumulado {
        private final String nombre;
        private long unidades;
        private long ingresos;

        private Acumulado(String nombre) { this.nombre = nombre; }
    }
}
