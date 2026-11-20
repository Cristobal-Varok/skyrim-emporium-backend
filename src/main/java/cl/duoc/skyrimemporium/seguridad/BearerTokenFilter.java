package cl.duoc.skyrimemporium.seguridad;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import cl.duoc.skyrimemporium.modelo.Usuario;
import cl.duoc.skyrimemporium.repositorio.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class BearerTokenFilter extends OncePerRequestFilter {

    private final TokenService tokens;
    private final UsuarioRepository usuarios;

    public BearerTokenFilter(TokenService tokens, UsuarioRepository usuarios) {
        this.tokens = tokens;
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String encabezado = request.getHeader("Authorization");
        if (encabezado != null && encabezado.startsWith("Bearer ")) {
            String run = tokens.buscarRun(encabezado.substring(7).trim());
            if (run != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Usuario usuario = usuarios.findById(run).orElse(null);
                if (usuario != null) {
                    var autoridad = new SimpleGrantedAuthority(
                            "ROLE_" + usuario.getTipo().toUpperCase(Locale.ROOT));
                    var autenticacion = new UsernamePasswordAuthenticationToken(
                            usuario.getRun(), null, List.of(autoridad));
                    SecurityContextHolder.getContext().setAuthentication(autenticacion);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
