package cl.duoc.skyrimemporium.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerfilRequest(
        @Schema(example = "Juan")
        @NotBlank @Size(max = 50) String nombre,
        @Schema(example = "Perez Gonzalez")
        @NotBlank @Size(max = 100) String apellidos,
        @Schema(example = "juan.perez@example.cl")
        @NotBlank @Email @Size(max = 100) String correo) {
}
