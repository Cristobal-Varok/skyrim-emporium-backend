package cl.duoc.skyrimemporium.dto;

import java.time.LocalDate;

import cl.duoc.skyrimemporium.modelo.Usuario;

public record UsuarioResponse(String run, String nombre, String apellidos, String correo,
        LocalDate fechaNacimiento, String tipo, String region, String comuna, String direccion) {

    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(usuario.getRun(), usuario.getNombre(), usuario.getApellidos(),
                usuario.getCorreo(), usuario.getFechaNacimiento(), usuario.getTipo(),
                usuario.getRegion(), usuario.getComuna(), usuario.getDireccion());
    }
}
