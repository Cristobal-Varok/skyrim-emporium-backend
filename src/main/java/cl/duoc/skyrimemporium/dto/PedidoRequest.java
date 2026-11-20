package cl.duoc.skyrimemporium.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record PedidoRequest(
        @NotEmpty(message = "El pedido no tiene productos")
        @Valid List<LineaPedido> lineas,
        @Size(max = 120, message = "El nombre admite hasta 120 caracteres") String cliente,
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 160, message = "El correo admite hasta 160 caracteres") String correo,
        @Size(max = 300) String calle,
        @Size(max = 100) String departamento,
        @Size(max = 100) String region,
        @Size(max = 100) String comuna,
        @Size(max = 500) String indicaciones) {

    public PedidoRequest(List<LineaPedido> lineas, String cliente, String correo) {
        this(lineas, cliente, correo, null, null, null, null, null);
    }
}
