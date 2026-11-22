package cl.duoc.skyrimemporium.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import cl.duoc.skyrimemporium.modelo.Pedido;

public record PedidoResponse(
        @Schema(example = "1001") Long id,
        @Schema(example = "2026-11-21T20:45:00") LocalDateTime fecha,
        @Schema(example = "2") Integer unidades,
        @Schema(example = "300") Long total,
        @Schema(example = "Juan Perez") String cliente,
        @Schema(example = "12345678-5") String clienteRun,
        @Schema(example = "juan.perez@example.cl") String correo,
        @Schema(example = "Pagada") String estado,
        @Schema(example = "Avenida Providencia 1234") String calle,
        @Schema(example = "502") String departamento,
        @Schema(example = "Metropolitana de Santiago") String region,
        @Schema(example = "Santiago") String comuna,
        @Schema(example = "Dejar en conserjeria") String indicaciones,
        @ArraySchema(schema = @Schema(implementation = PedidoItemResponse.class)) List<PedidoItemResponse> items) {

    public static PedidoResponse desde(Pedido pedido) {
        return new PedidoResponse(pedido.getId(), pedido.getFecha(), pedido.getUnidades(), pedido.getTotal(),
                pedido.getCliente(), pedido.getClienteRun(), pedido.getCorreo(), pedido.getEstado(),
                pedido.getCalle(), pedido.getDepartamento(), pedido.getRegion(), pedido.getComuna(),
                pedido.getIndicaciones(), pedido.getItems().stream().map(PedidoItemResponse::desde).toList());
    }
}
