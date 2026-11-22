package cl.duoc.skyrimemporium.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import cl.duoc.skyrimemporium.modelo.Usuario;

public record UsuarioResponse(
        @Schema(example = "12345678-5") String run,
        @Schema(example = "Juan") String nombre,
        @Schema(example = "Perez Gonzalez") String apellidos,
        @Schema(example = "juan.perez@example.cl") String correo,
        @Schema(example = "1998-06-15") LocalDate fechaNacimiento,
        @Schema(example = "Cliente") String tipo,
        @Schema(example = "Metropolitana de Santiago") String region,
        @Schema(example = "Santiago") String comuna,
        @Schema(example = "Avenida Providencia 1234") String direccion) {

    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(usuario.getRun(), usuario.getNombre(), usuario.getApellidos(),
                usuario.getCorreo(), usuario.getFechaNacimiento(), usuario.getTipo(),
                usuario.getRegion(), usuario.getComuna(), usuario.getDireccion());
    }
}
