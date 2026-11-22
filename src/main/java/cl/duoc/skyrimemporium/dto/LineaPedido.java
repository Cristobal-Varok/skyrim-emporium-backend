package cl.duoc.skyrimemporium.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LineaPedido(
        @Schema(example = "curar-enfermedad")
        @NotBlank(message = "Falta el identificador del producto") String productoId,
        @Schema(example = "2")
        @NotNull(message = "Falta la cantidad")
        @Positive(message = "La cantidad debe ser mayor que cero") Integer cantidad,
        @Schema(example = "alquimia")
        String gremio) {

    public LineaPedido(String productoId, Integer cantidad) {
        this(productoId, cantidad, null);
    }
}
