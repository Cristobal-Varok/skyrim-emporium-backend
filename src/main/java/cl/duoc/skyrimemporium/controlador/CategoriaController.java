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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.skyrimemporium.dto.CategoriaRequest;
import cl.duoc.skyrimemporium.servicio.CategoriaService;
import cl.duoc.skyrimemporium.servicio.CategoriaService.CategoriaResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService servicio;

    public CategoriaController(CategoriaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<CategoriaResponse> listar() {
        return servicio.listar();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse crear(@Valid @RequestBody CategoriaRequest solicitud) {
        return servicio.crear(solicitud);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public CategoriaResponse actualizar(@PathVariable String id, @Valid @RequestBody CategoriaRequest solicitud) {
        return servicio.actualizar(id, solicitud);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        servicio.eliminar(id);
    }
}
