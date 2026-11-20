package cl.duoc.skyrimemporium.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private static final Duration DURACION = Duration.ofHours(8);
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, Sesion> sesiones = new ConcurrentHashMap<>();

    public TokenEmitido crear(String run) {
        Instant ahora = Instant.now();
        sesiones.entrySet().removeIf(entry -> !entry.getValue().vence().isAfter(ahora));
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant vence = ahora.plus(DURACION);
        sesiones.put(resumir(token), new Sesion(run, vence));
        return new TokenEmitido(token, vence);
    }

    public String buscarRun(String token) {
        String huella = resumir(token);
        Sesion sesion = sesiones.get(huella);
        if (sesion == null) return null;
        if (!sesion.vence().isAfter(Instant.now())) {
            sesiones.remove(huella, sesion);
            return null;
        }
        return sesion.run();
    }

    public void revocar(String token) {
        if (token != null && !token.isBlank()) sesiones.remove(resumir(token));
    }

    private String resumir(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible", ex);
        }
    }

    private record Sesion(String run, Instant vence) { }
    public record TokenEmitido(String valor, Instant vence) { }
}
