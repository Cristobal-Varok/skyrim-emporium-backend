package cl.duoc.skyrimemporium.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @Schema(example = "12345678-5")
        @NotBlank(message = "El RUN es obligatorio") @Size(max = 12) String run,
        @Schema(example = "Juan")
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 50) String nombre,
        @Schema(example = "Perez Gonzalez")
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 100) String apellidos,
        @Schema(example = "juan.perez@example.cl")
        @NotBlank(message = "El correo es obligatorio") @Email @Size(max = 100) String correo,
        @Schema(example = "MiClaveSegura123!")
        @Size(min = 4, max = 72, message = "La contraseña debe tener entre 4 y 72 caracteres") String contrasena,
        @Schema(example = "1998-06-15")
        LocalDate fechaNacimiento,
        @Schema(example = "Cliente")
        @Size(max = 20) String tipo,
        @Schema(example = "Metropolitana de Santiago")
        @Size(max = 100) String region,
        @Schema(example = "Santiago")
        @Size(max = 100) String comuna,
        @Schema(example = "Avenida Providencia 1234")
        @Size(max = 300) String direccion) {
}
