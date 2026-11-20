package cl.duoc.skyrimemporium.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pedido_items")
public class PedidoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idInterno;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(nullable = false)
    private String productoId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Integer precio;

    @Column(nullable = false)
    private Long total;

    public PedidoItem() {
    }

    public PedidoItem(Pedido pedido, String productoId, String nombre, Integer cantidad, Integer precio) {
        this.pedido = pedido;
        this.productoId = productoId;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.precio = precio;
        this.total = Math.multiplyExact((long) precio, cantidad);
    }

    public String getId() { return productoId; }
    public Long getIdInterno() { return idInterno; }
    public Pedido getPedido() { return pedido; }
    public String getNombre() { return nombre; }
    public Integer getCantidad() { return cantidad; }
    public Integer getPrecio() { return precio; }
    public Long getTotal() { return total; }
}
