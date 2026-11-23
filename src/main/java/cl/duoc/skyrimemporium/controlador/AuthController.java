package cl.duoc.skyrimemporium.controlador;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import cl.duoc.skyrimemporium.dto.AuthResponse;
import cl.duoc.skyrimemporium.dto.LoginRequest;
import cl.duoc.skyrimemporium.dto.UsuarioResponse;
import cl.duoc.skyrimemporium.servicio.AuthService;
import cl.duoc.skyrimemporium.servicio.UsuarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String COOKIE_REFRESH = "skyrim_refresh";

    private final AuthService auth;
    private final UsuarioService usuarios;
    private final boolean cookieSeguro;

    public AuthController(AuthService auth, UsuarioService usuarios,
            @Value("${security.jwt.refresh-cookie-secure:false}") boolean cookieSeguro) {
        this.auth = auth;
        this.usuarios = usuarios;
        this.cookieSeguro = cookieSeguro;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> iniciarSesion(@Valid @RequestBody LoginRequest solicitud) {
        return responder(auth.iniciarSesion(solicitud));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> renovar(
            @CookieValue(name = COOKIE_REFRESH, required = false) String refreshToken) {
        return responder(auth.renovar(refreshToken));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public UsuarioResponse sesion(@AuthenticationPrincipal String run) {
        return UsuarioResponse.desde(usuarios.buscar(run));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> cerrarSesion(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String encabezado,
            @CookieValue(name = COOKIE_REFRESH, required = false) String refreshToken) {
        String accessToken = encabezado != null && encabezado.startsWith("Bearer ")
                ? encabezado.substring(7).trim() : null;
        auth.cerrarSesion(accessToken, refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .build();
    }

    private ResponseEntity<AuthResponse> responder(AuthService.SesionAuth sesion) {
        Duration maxAge = Duration.between(Instant.now(), sesion.venceRefresh());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(sesion.refreshToken(), maxAge).toString())
                .body(sesion.respuesta());
    }

    private ResponseCookie cookie(String valor, Duration maxAge) {
        return ResponseCookie.from(COOKIE_REFRESH, valor)
                .httpOnly(true)
                .secure(cookieSeguro)
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .build();
    }
}
