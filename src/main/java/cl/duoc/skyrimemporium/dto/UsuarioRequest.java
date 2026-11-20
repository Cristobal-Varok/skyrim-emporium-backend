package cl.duoc.skyrimemporium.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank(message = "El RUN es obligatorio") @Size(max = 12) String run,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 50) String nombre,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 100) String apellidos,
        @NotBlank(message = "El correo es obligatorio") @Email @Size(max = 100) String correo,
        @Size(min = 4, max = 72, message = "La contraseña debe tener entre 4 y 72 caracteres") String contrasena,
        LocalDate fechaNacimiento,
        @Size(max = 20) String tipo,
        @Size(max = 100) String region,
        @Size(max = 100) String comuna,
        @Size(max = 300) String direccion) {
}
