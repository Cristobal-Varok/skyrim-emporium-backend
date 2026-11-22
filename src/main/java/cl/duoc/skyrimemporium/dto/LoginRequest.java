package cl.duoc.skyrimemporium.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "vale.rodrigueza@duoc.cl")
        @NotBlank @Email String correo,
        @Schema(example = "1234567")
        @NotBlank String contrasena) {
}
