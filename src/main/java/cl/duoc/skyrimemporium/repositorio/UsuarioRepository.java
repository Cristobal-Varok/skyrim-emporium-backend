package cl.duoc.skyrimemporium.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.skyrimemporium.modelo.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
    boolean existsByCorreoIgnoreCase(String correo);
    long countByTipoIgnoreCase(String tipo);
}
