package cl.duoc.skyrimemporium.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ReporteVentaProducto(
        @Schema(example = "Pocion para curar enfermedades") String nombre,
        @Schema(example = "48") long unidades,
        @Schema(example = "7200") long ingresos) {
}
