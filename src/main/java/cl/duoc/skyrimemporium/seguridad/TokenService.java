package cl.duoc.skyrimemporium.seguridad;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.duoc.skyrimemporium.error.CredencialesInvalidasException;

@Service
public class TokenService {

    private static final String ALGORITMO = "HmacSHA256";
    private static final String TIPO_ACCESS = "access";
    private static final String TIPO_REFRESH = "refresh";
    private static final Base64.Encoder CODIFICADOR = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODIFICADOR = Base64.getUrlDecoder();

    private final byte[] clave;
    private final Duration duracionAccess;
    private final Duration duracionRefresh;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Instant> refreshActivos = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> accessRevocados = new ConcurrentHashMap<>();

    public TokenService(
            @Value("${security.jwt.secret}") String secretoBase64,
            @Value("${security.jwt.access-token-ttl:PT15M}") Duration duracionAccess,
            @Value("${security.jwt.refresh-token-ttl:PT8H}") Duration duracionRefresh,
            ObjectMapper objectMapper) {
        try {
            this.clave = Base64.getDecoder().decode(secretoBase64);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT_SECRET debe ser una clave Base64 válida", ex);
        }
        if (clave.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe contener al menos 32 bytes");
        }
        if (duracionAccess.isZero() || duracionAccess.isNegative()
                || duracionRefresh.compareTo(duracionAccess) <= 0) {
            throw new IllegalStateException("Las duraciones JWT configuradas no son válidas");
        }
        this.duracionAccess = duracionAccess;
        this.duracionRefresh = duracionRefresh;
        this.objectMapper = objectMapper;
    }

    public TokensEmitidos crear(String run) {
        limpiarExpirados();
        Instant ahora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Token access = crearToken(run, TIPO_ACCESS, ahora, duracionAccess);
        Token refresh = crearToken(run, TIPO_REFRESH, ahora, duracionRefresh);
        refreshActivos.put(refresh.id(), refresh.vence());
        return new TokensEmitidos(access.valor(), refresh.valor(), access.vence(), refresh.vence());
    }

    public TokensEmitidos renovar(String refreshToken) {
        try {
            Claims claims = verificar(refreshToken, TIPO_REFRESH);
            Instant vencimiento = refreshActivos.get(claims.id());
            if (vencimiento == null || !vencimiento.equals(claims.vence())
                    || !refreshActivos.remove(claims.id(), vencimiento)) {
                throw new TokenInvalidoException();
            }
            return crear(claims.run());
        } catch (TokenInvalidoException ex) {
            throw new CredencialesInvalidasException();
        }
    }

    public String buscarRun(String accessToken) {
        try {
            Claims claims = verificar(accessToken, TIPO_ACCESS);
            return accessRevocados.containsKey(claims.id()) ? null : claims.run();
        } catch (TokenInvalidoException ex) {
            return null;
        }
    }

    public void revocarAccess(String accessToken) {
        try {
            Claims claims = verificar(accessToken, TIPO_ACCESS);
            accessRevocados.put(claims.id(), claims.vence());
        } catch (TokenInvalidoException ex) {
            // An invalid or expired access token is already unusable.
        }
    }

    public void revocarRefresh(String refreshToken) {
        try {
            Claims claims = verificar(refreshToken, TIPO_REFRESH);
            refreshActivos.remove(claims.id(), claims.vence());
        } catch (TokenInvalidoException ex) {
            // An invalid or expired refresh token is already unusable.
        }
    }

    private Token crearToken(String run, String tipo, Instant ahora, Duration duracion) {
        Instant vence = ahora.plus(duracion);
        String id = UUID.randomUUID().toString();
        String encabezado = codificar(Map.of("alg", "HS256", "typ", "JWT"));
        String contenido = codificar(Map.of(
                "sub", run,
                "jti", id,
                "typ", tipo,
                "iat", ahora.getEpochSecond(),
                "exp", vence.getEpochSecond()));
        String firmado = encabezado + "." + contenido;
        return new Token(firmado + "." + CODIFICADOR.encodeToString(firmar(firmado)), id, vence);
    }

    private Claims verificar(String valor, String tipoEsperado) {
        if (valor == null || valor.isBlank() || valor.length() > 8192) {
            throw new TokenInvalidoException();
        }
        String[] partes = valor.split("\\.", -1);
        if (partes.length != 3) throw new TokenInvalidoException();
        try {
            JsonNode encabezado = objectMapper.readTree(DECODIFICADOR.decode(partes[0]));
            JsonNode contenido = objectMapper.readTree(DECODIFICADOR.decode(partes[1]));
            byte[] firmaRecibida = DECODIFICADOR.decode(partes[2]);
            byte[] firmaEsperada = firmar(partes[0] + "." + partes[1]);
            if (!"HS256".equals(encabezado.path("alg").asText())
                    || !MessageDigest.isEqual(firmaEsperada, firmaRecibida)) {
                throw new TokenInvalidoException();
            }

            String run = contenido.path("sub").asText();
            String id = contenido.path("jti").asText();
            String tipo = contenido.path("typ").asText();
            JsonNode exp = contenido.get("exp");
            if (run.isBlank() || id.isBlank() || !tipoEsperado.equals(tipo)
                    || exp == null || !exp.canConvertToLong()) {
                throw new TokenInvalidoException();
            }
            Instant vence = Instant.ofEpochSecond(exp.asLong());
            if (!vence.isAfter(Instant.now())) throw new TokenInvalidoException();
            return new Claims(run, id, vence);
        } catch (IllegalArgumentException | IOException ex) {
            throw new TokenInvalidoException();
        }
    }

    private String codificar(Object valor) {
        try {
            return CODIFICADOR.encodeToString(objectMapper.writeValueAsBytes(valor));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar el token JWT", ex);
        }
    }

    private byte[] firmar(String contenido) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(clave, ALGORITMO));
            return mac.doFinal(contenido.getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo firmar el token JWT", ex);
        }
    }

    private void limpiarExpirados() {
        Instant ahora = Instant.now();
        refreshActivos.entrySet().removeIf(entry -> !entry.getValue().isAfter(ahora));
        accessRevocados.entrySet().removeIf(entry -> !entry.getValue().isAfter(ahora));
    }

    public record TokensEmitidos(
            String accessToken,
            String refreshToken,
            Instant venceAccess,
            Instant venceRefresh) { }

    private record Token(String valor, String id, Instant vence) { }
    private record Claims(String run, String id, Instant vence) { }

    private static final class TokenInvalidoException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
