package cl.duoc.skyrimemporium.dto;

import java.util.List;

public record ReporteAdminResponse(long ingresos, long ordenes, long productosCriticos,
        List<ReporteVentaProducto> masVendidos, List<PedidoResponse> ordenesRecientes) {
}
