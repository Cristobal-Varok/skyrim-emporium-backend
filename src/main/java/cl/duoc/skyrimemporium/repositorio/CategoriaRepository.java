package cl.duoc.skyrimemporium.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.skyrimemporium.modelo.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, String> {
    List<Categoria> findAllByOrderByGremioAscTituloAsc();
    boolean existsByGremioIgnoreCaseAndTituloIgnoreCase(String gremio, String titulo);
}
