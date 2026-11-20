package cl.duoc.skyrimemporium.dto;

import java.time.LocalDateTime;
import java.util.List;

import cl.duoc.skyrimemporium.modelo.Pedido;

public record PedidoResponse(Long id, LocalDateTime fecha, Integer unidades, Long total, String cliente,
        String clienteRun, String correo, String estado, String calle, String departamento, String region,
        String comuna, String indicaciones, List<PedidoItemResponse> items) {

    public static PedidoResponse desde(Pedido pedido) {
        return new PedidoResponse(pedido.getId(), pedido.getFecha(), pedido.getUnidades(), pedido.getTotal(),
                pedido.getCliente(), pedido.getClienteRun(), pedido.getCorreo(), pedido.getEstado(),
                pedido.getCalle(), pedido.getDepartamento(), pedido.getRegion(), pedido.getComuna(),
                pedido.getIndicaciones(), pedido.getItems().stream().map(PedidoItemResponse::desde).toList());
    }
}
