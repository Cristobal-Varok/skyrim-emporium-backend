package cl.duoc.skyrimemporium.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record PedidoRequest(
        @ArraySchema(
                schema = @Schema(implementation = LineaPedido.class),
                arraySchema = @Schema(example = "[{\"productoId\":\"curar-enfermedad\",\"cantidad\":2,\"gremio\":\"alquimia\"}]"))
        @NotEmpty(message = "El pedido no tiene productos")
        @Valid List<LineaPedido> lineas,
        @Schema(example = "Juan Perez")
        @Size(max = 120, message = "El nombre admite hasta 120 caracteres") String cliente,
        @Schema(example = "juan.perez@example.cl")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 160, message = "El correo admite hasta 160 caracteres") String correo,
        @Schema(example = "Avenida Providencia 1234")
        @Size(max = 300) String calle,
        @Schema(example = "502")
        @Size(max = 100) String departamento,
        @Schema(example = "Metropolitana de Santiago")
        @Size(max = 100) String region,
        @Schema(example = "Santiago")
        @Size(max = 100) String comuna,
        @Schema(example = "Dejar en conserjeria")
        @Size(max = 500) String indicaciones) {

    public PedidoRequest(List<LineaPedido> lineas, String cliente, String correo) {
        this(lineas, cliente, correo, null, null, null, null, null);
    }
}
