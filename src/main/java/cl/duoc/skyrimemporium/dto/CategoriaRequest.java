package cl.duoc.skyrimemporium.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @Schema(example = "alquimia")
        @NotBlank(message = "El gremio es obligatorio")
        @Size(max = 40, message = "El gremio admite hasta 40 caracteres") String gremio,
        @Schema(example = "Pociones de prueba")
        @NotBlank(message = "El nombre de categoría es obligatorio")
        @Size(max = 80, message = "La categoría admite hasta 80 caracteres") String titulo) {
}
