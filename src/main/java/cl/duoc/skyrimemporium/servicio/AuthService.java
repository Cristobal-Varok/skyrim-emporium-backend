package cl.duoc.skyrimemporium.servicio;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import cl.duoc.skyrimemporium.dto.AuthResponse;
import cl.duoc.skyrimemporium.dto.LoginRequest;
import cl.duoc.skyrimemporium.dto.UsuarioResponse;
import cl.duoc.skyrimemporium.error.CredencialesInvalidasException;
import cl.duoc.skyrimemporium.modelo.Usuario;
import cl.duoc.skyrimemporium.repositorio.UsuarioRepository;
import cl.duoc.skyrimemporium.seguridad.TokenService;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    public SesionAuth iniciarSesion(LoginRequest solicitud) {
        Usuario usuario = usuarios.findByCorreoIgnoreCase(solicitud.correo().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(CredencialesInvalidasException::new);
        if (!passwordEncoder.matches(solicitud.contrasena(), usuario.getContrasenaHash())) {
            throw new CredencialesInvalidasException();
        }
        return crearSesion(usuario, tokens.crear(usuario.getRun()));
    }

    public SesionAuth renovar(String refreshToken) {
        TokenService.TokensEmitidos emitidos = tokens.renovar(refreshToken);
        String run = tokens.buscarRun(emitidos.accessToken());
        if (run == null) throw new CredencialesInvalidasException();
        Usuario usuario = usuarios.findById(run).orElseThrow(CredencialesInvalidasException::new);
        return crearSesion(usuario, emitidos);
    }

    public void cerrarSesion(String accessToken, String refreshToken) {
        tokens.revocarAccess(accessToken);
        tokens.revocarRefresh(refreshToken);
    }

    private SesionAuth crearSesion(Usuario usuario, TokenService.TokensEmitidos tokensEmitidos) {
        AuthResponse respuesta = new AuthResponse(
                tokensEmitidos.accessToken(),
                "Bearer",
                tokensEmitidos.venceAccess(),
                UsuarioResponse.desde(usuario));
        return new SesionAuth(respuesta, tokensEmitidos.refreshToken(), tokensEmitidos.venceRefresh());
    }

    public record SesionAuth(AuthResponse respuesta, String refreshToken, java.time.Instant venceRefresh) { }
}
