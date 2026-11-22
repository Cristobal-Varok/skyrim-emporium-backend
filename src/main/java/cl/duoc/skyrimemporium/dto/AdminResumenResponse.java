package cl.duoc.skyrimemporium.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminResumenResponse(
        @Schema(example = "125") long ordenes,
        @Schema(example = "187500") long ingresos,
        @Schema(example = "558") long productos,
        @Schema(example = "32") long usuarios) {
}
