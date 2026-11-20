package cl.duoc.skyrimemporium.dto;

import cl.duoc.skyrimemporium.modelo.PedidoItem;

public record PedidoItemResponse(String id, String nombre, Integer cantidad, Integer precio, Long total) {

    public static PedidoItemResponse desde(PedidoItem item) {
        return new PedidoItemResponse(item.getId(), item.getNombre(), item.getCantidad(),
                item.getPrecio(), item.getTotal());
    }
}
