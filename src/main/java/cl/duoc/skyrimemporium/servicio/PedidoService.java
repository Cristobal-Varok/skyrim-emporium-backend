package cl.duoc.skyrimemporium.servicio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.skyrimemporium.dto.LineaPedido;
import cl.duoc.skyrimemporium.dto.PedidoRequest;
import cl.duoc.skyrimemporium.error.AccesoDenegadoException;
import cl.duoc.skyrimemporium.error.RecursoNoEncontradoException;
import cl.duoc.skyrimemporium.error.StockInsuficienteException;
import cl.duoc.skyrimemporium.modelo.Pedido;
import cl.duoc.skyrimemporium.modelo.PedidoItem;
import cl.duoc.skyrimemporium.modelo.Producto;
import cl.duoc.skyrimemporium.modelo.Usuario;
import cl.duoc.skyrimemporium.repositorio.PedidoRepository;
import cl.duoc.skyrimemporium.repositorio.ProductoRepository;
import cl.duoc.skyrimemporium.repositorio.UsuarioRepository;

@Service
public class PedidoService {

    private final ProductoRepository productos;
    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;

    public PedidoService(ProductoRepository productos, PedidoRepository pedidos, UsuarioRepository usuarios) {
        this.productos = productos;
        this.pedidos = pedidos;
        this.usuarios = usuarios;
    }

    @Transactional
    public Pedido crear(PedidoRequest solicitud, String clienteRun) {
        if (clienteRun == null || clienteRun.isBlank()) {
            throw new AccesoDenegadoException("Debes iniciar sesión para confirmar el pedido");
        }
        Map<String, Integer> cantidades = new LinkedHashMap<>();
        for (LineaPedido linea : solicitud.lineas()) {
            Producto producto = resolverProducto(linea);
            cantidades.merge(producto.getId(), linea.cantidad(), Math::addExact);
        }

        Usuario cuenta = usuarios.findById(clienteRun)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la cuenta del comprador"));
        String cliente = (cuenta.getNombre() + " " + cuenta.getApellidos()).trim();
        String correo = cuenta.getCorreo();
        int unidades = 0;
        long total = 0;
        List<DetallePedido> detalles = new ArrayList<>();
        for (Map.Entry<String, Integer> item : cantidades.entrySet()) {
            Producto producto = productos.findByIdForUpdate(item.getKey())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe el producto " + item.getKey()));
            int cantidad = item.getValue();
            if (producto.getStock() < cantidad) {
                throw new StockInsuficienteException("No hay stock suficiente de «" + producto.getName()
                        + "»: quedan " + producto.getStock() + " unidades.");
            }
            producto.setStock(producto.getStock() - cantidad);
            unidades = Math.addExact(unidades, cantidad);
            total = Math.addExact(total, Math.multiplyExact((long) producto.getPrice(), cantidad));
            detalles.add(new DetallePedido(producto.getId(), producto.getName(), cantidad, producto.getPrice()));
        }
        Pedido orden = new Pedido(LocalDateTime.now(), unidades, total, cliente, correo, clienteRun,
                solicitud.calle(), solicitud.departamento(), solicitud.region(), solicitud.comuna(),
                solicitud.indicaciones());
        detalles.forEach(item -> orden.agregarItem(new PedidoItem(orden, item.productoId(),
                item.nombre(), item.cantidad(), item.precio())));
        return pedidos.save(orden);
    }

    @Transactional(readOnly = true)
    public Pedido buscar(Long id) {
        return pedidos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pedido " + id));
    }

    private Producto resolverProducto(LineaPedido linea) {
        String id = linea.productoId();
        if (id.contains("::")) {
            return productos.findById(id)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + id));
        }
        if (linea.gremio() != null && !linea.gremio().isBlank()) {
            String idCompleto = linea.gremio().trim() + "::" + id;
            Producto compuesto = productos.findById(idCompleto).orElse(null);
            if (compuesto != null) return compuesto;
            Producto producto = productos.findById(id).orElse(null);
            if (producto != null && producto.getGremio().equalsIgnoreCase(linea.gremio().trim())) return producto;
            throw new RecursoNoEncontradoException("No existe el producto " + idCompleto);
        }
        var coincidencias = productos.findAll().stream()
                .filter(p -> p.getId().endsWith("::" + id))
                .toList();
        if (coincidencias.size() > 1) {
            throw new IllegalArgumentException("El producto es ambiguo; especifica su gremio");
        }
        if (coincidencias.isEmpty()) {
            throw new RecursoNoEncontradoException("No existe el producto " + id);
        }
        return coincidencias.get(0);
    }

    private record DetallePedido(String productoId, String nombre, Integer cantidad, Integer precio) { }
}
