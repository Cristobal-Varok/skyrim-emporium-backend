package cl.duoc.skyrimemporium.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
        @Schema(example = "token JWT generado al iniciar sesion") String accessToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(example = "2026-11-21T21:15:00Z") Instant expiresAt,
        UsuarioResponse usuario) {
}
