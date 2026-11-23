package cl.duoc.skyrimemporium.controlador;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import cl.duoc.skyrimemporium.dto.PerfilRequest;
import cl.duoc.skyrimemporium.dto.UsuarioRequest;
import cl.duoc.skyrimemporium.dto.UsuarioResponse;
import cl.duoc.skyrimemporium.servicio.UsuarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SecurityRequirement(name = "bearerAuth")
    public List<UsuarioResponse> listar() {
        return servicio.listar().stream().map(UsuarioResponse::desde).toList();
    }

    @GetMapping("/{run}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SecurityRequirement(name = "bearerAuth")
    public UsuarioResponse obtener(@PathVariable String run) {
        return UsuarioResponse.desde(servicio.buscar(run));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest solicitud) {
        return UsuarioResponse.desde(servicio.crear(solicitud, false));
    }

    @PutMapping("/{run}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SecurityRequirement(name = "bearerAuth")
    public UsuarioResponse actualizar(@PathVariable String run, @Valid @RequestBody UsuarioRequest solicitud) {
        return UsuarioResponse.desde(servicio.actualizar(run, solicitud));
    }

    @DeleteMapping("/{run}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String run, @AuthenticationPrincipal String actorRun) {
        servicio.eliminar(run, actorRun);
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@Valid @RequestBody UsuarioRequest solicitud) {
        return UsuarioResponse.desde(servicio.crear(solicitud, true));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public UsuarioResponse perfil(@AuthenticationPrincipal String run) {
        return UsuarioResponse.desde(servicio.buscar(run));
    }

    @PutMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public UsuarioResponse actualizarPerfil(@AuthenticationPrincipal String run,
            @Valid @RequestBody PerfilRequest solicitud) {
        return UsuarioResponse.desde(servicio.actualizarPerfil(run, solicitud));
    }
}
