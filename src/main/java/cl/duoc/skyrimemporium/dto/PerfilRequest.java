package cl.duoc.skyrimemporium.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerfilRequest(
        @NotBlank @Size(max = 50) String nombre,
        @NotBlank @Size(max = 100) String apellidos,
        @NotBlank @Email @Size(max = 100) String correo) {
}
