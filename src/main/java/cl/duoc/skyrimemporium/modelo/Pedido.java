package cl.duoc.skyrimemporium.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fecha;
    private Integer unidades;
    private Long total;
    private String cliente;
    private String correo;
    private String clienteRun;
    private String estado;
    private String calle;
    private String departamento;
    private String region;
    private String comuna;
    private String indicaciones;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoItem> items = new ArrayList<>();

    public Pedido() {
    }

    public Pedido(LocalDateTime fecha, Integer unidades, Long total, String cliente, String correo) {
        this(fecha, unidades, total, cliente, correo, null, null, null, null, null, null);
    }

    public Pedido(LocalDateTime fecha, Integer unidades, Long total, String cliente, String correo,
            String clienteRun, String calle, String departamento, String region, String comuna,
            String indicaciones) {
        this.fecha = fecha;
        this.unidades = unidades;
        this.total = total;
        this.cliente = cliente;
        this.correo = correo;
        this.clienteRun = clienteRun;
        this.estado = "Pagada";
        this.calle = calle;
        this.departamento = departamento;
        this.region = region;
        this.comuna = comuna;
        this.indicaciones = indicaciones;
    }

    public Long getId() { return id; }
    public LocalDateTime getFecha() { return fecha; }
    public Integer getUnidades() { return unidades; }
    public Long getTotal() { return total; }
    public String getCliente() { return cliente; }
    public String getCorreo() { return correo; }
    public String getClienteRun() { return clienteRun; }
    public String getEstado() { return estado; }
    public String getCalle() { return calle; }
    public String getDepartamento() { return departamento; }
    public String getRegion() { return region; }
    public String getComuna() { return comuna; }
    public String getIndicaciones() { return indicaciones; }
    public List<PedidoItem> getItems() { return items; }

    public void agregarItem(PedidoItem item) {
        items.add(item);
    }
}
