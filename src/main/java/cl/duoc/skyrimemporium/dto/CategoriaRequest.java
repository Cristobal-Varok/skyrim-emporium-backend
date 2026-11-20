package cl.duoc.skyrimemporium.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank(message = "El gremio es obligatorio")
        @Size(max = 40, message = "El gremio admite hasta 40 caracteres") String gremio,
        @NotBlank(message = "El nombre de categoría es obligatorio")
        @Size(max = 80, message = "La categoría admite hasta 80 caracteres") String titulo) {
}
