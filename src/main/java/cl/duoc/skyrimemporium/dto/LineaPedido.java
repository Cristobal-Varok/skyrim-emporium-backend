package cl.duoc.skyrimemporium.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LineaPedido(
        @NotBlank(message = "Falta el identificador del producto") String productoId,
        @NotNull(message = "Falta la cantidad")
        @Positive(message = "La cantidad debe ser mayor que cero") Integer cantidad,
        String gremio) {

    public LineaPedido(String productoId, Integer cantidad) {
        this(productoId, cantidad, null);
    }
}
