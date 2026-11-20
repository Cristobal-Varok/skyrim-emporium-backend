package cl.duoc.skyrimemporium.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.duoc.skyrimemporium.modelo.Producto;
import jakarta.persistence.LockModeType;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") String id);

    long countByGremioIgnoreCaseAndCategoriaSeccionIgnoreCase(String gremio, String categoriaSeccion);
    boolean existsByGremioIgnoreCaseAndCategoriaSeccionIgnoreCase(String gremio, String categoriaSeccion);
    List<Producto> findAllByGremioIgnoreCaseAndCategoriaSeccionIgnoreCase(String gremio, String categoriaSeccion);
    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, String id);
}
