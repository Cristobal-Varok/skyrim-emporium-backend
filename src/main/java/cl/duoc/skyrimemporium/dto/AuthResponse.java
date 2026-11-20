package cl.duoc.skyrimemporium.dto;

import java.time.Instant;

public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, UsuarioResponse usuario) {
}
