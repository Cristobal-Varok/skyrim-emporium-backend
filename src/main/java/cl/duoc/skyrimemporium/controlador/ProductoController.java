package cl.duoc.skyrimemporium.controlador;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import cl.duoc.skyrimemporium.modelo.Producto;
import cl.duoc.skyrimemporium.servicio.ProductoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {

    private final ProductoService servicio;

    public ProductoController(ProductoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Producto> listar(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String gremio) {
        return servicio.listar(gremio, categoria);
    }

    @GetMapping("/{id}")
    public Producto obtener(@PathVariable String id) {
        return servicio.buscar(id);
    }

    @GetMapping("/stock-critico")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SecurityRequirement(name = "bearerAuth")
    public List<Producto> stockCritico() {
        return servicio.stockCritico();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    public Producto crear(@Valid @RequestBody Producto producto) {
        return servicio.crear(producto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SecurityRequirement(name = "bearerAuth")
    public Producto actualizar(@PathVariable String id, @Valid @RequestBody Producto producto) {
        return servicio.actualizar(id, producto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        servicio.eliminar(id);
    }
}
