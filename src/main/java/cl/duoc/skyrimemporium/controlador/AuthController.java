package cl.duoc.skyrimemporium.controlador;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.skyrimemporium.dto.AuthResponse;
import cl.duoc.skyrimemporium.dto.LoginRequest;
import cl.duoc.skyrimemporium.dto.UsuarioResponse;
import cl.duoc.skyrimemporium.servicio.AuthService;
import cl.duoc.skyrimemporium.servicio.UsuarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final UsuarioService usuarios;

    public AuthController(AuthService auth, UsuarioService usuarios) {
        this.auth = auth;
        this.usuarios = usuarios;
    }

    @PostMapping("/login")
    public AuthResponse iniciarSesion(@Valid @RequestBody LoginRequest solicitud) {
        return auth.iniciarSesion(solicitud);
    }

    @GetMapping("/me")
    public UsuarioResponse sesion(@AuthenticationPrincipal String run) {
        return UsuarioResponse.desde(usuarios.buscar(run));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrarSesion(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String encabezado) {
        if (encabezado != null && encabezado.startsWith("Bearer ")) {
            auth.cerrarSesion(encabezado.substring(7).trim());
        }
    }
}
