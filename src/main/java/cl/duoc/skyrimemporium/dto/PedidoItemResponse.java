package cl.duoc.skyrimemporium.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import cl.duoc.skyrimemporium.modelo.PedidoItem;

public record PedidoItemResponse(
        @Schema(example = "alquimia::curar-enfermedad") String id,
        @Schema(example = "Pocion para curar enfermedades") String nombre,
        @Schema(example = "2") Integer cantidad,
        @Schema(example = "150") Integer precio,
        @Schema(example = "300") Long total) {

    public static PedidoItemResponse desde(PedidoItem item) {
        return new PedidoItemResponse(item.getId(), item.getNombre(), item.getCantidad(),
                item.getPrecio(), item.getTotal());
    }
}
