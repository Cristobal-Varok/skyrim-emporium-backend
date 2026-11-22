package cl.duoc.skyrimemporium.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ReporteAdminResponse(
        @Schema(example = "187500") long ingresos,
        @Schema(example = "125") long ordenes,
        @Schema(example = "18") long productosCriticos,
        @ArraySchema(schema = @Schema(implementation = ReporteVentaProducto.class)) List<ReporteVentaProducto> masVendidos,
        @ArraySchema(schema = @Schema(implementation = PedidoResponse.class)) List<PedidoResponse> ordenesRecientes) {
}
