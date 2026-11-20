package cl.duoc.skyrimemporium.repositorio;

import cl.duoc.skyrimemporium.modelo.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    @EntityGraph(attributePaths = "items")
    List<Pedido> findAllByOrderByFechaDesc();

    @EntityGraph(attributePaths = "items")
    List<Pedido> findByClienteRunOrderByFechaDesc(String clienteRun);

    @EntityGraph(attributePaths = "items")
    Optional<Pedido> findById(Long id);
}
