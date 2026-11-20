package cl.duoc.skyrimemporium.controlador;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.skyrimemporium.dto.PedidoRequest;
import cl.duoc.skyrimemporium.dto.PedidoResponse;
import cl.duoc.skyrimemporium.error.AccesoDenegadoException;
import cl.duoc.skyrimemporium.modelo.Pedido;
import cl.duoc.skyrimemporium.repositorio.PedidoRepository;
import cl.duoc.skyrimemporium.servicio.PedidoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService servicio;
    private final PedidoRepository repositorio;

    public PedidoController(PedidoService servicio, PedidoRepository repositorio) {
        this.servicio = servicio;
        this.repositorio = repositorio;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<PedidoResponse> listar() {
        return repositorio.findAllByOrderByFechaDesc().stream().map(PedidoResponse::desde).toList();
    }

    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Long id, Authentication autenticacion) {
        Pedido pedido = servicio.buscar(id);
        verificarAcceso(pedido, autenticacion);
        return PedidoResponse.desde(pedido);
    }

    @GetMapping("/usuario/{run}")
    public List<PedidoResponse> historial(@PathVariable String run, Authentication autenticacion) {
        if (!esAdministrador(autenticacion) && !run.equals(autenticacion.getName())) {
            throw new AccesoDenegadoException("No puedes consultar el historial de otra cuenta");
        }
        return repositorio.findByClienteRunOrderByFechaDesc(run).stream().map(PedidoResponse::desde).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse crear(@Valid @RequestBody PedidoRequest solicitud,
            Authentication autenticacion) {
        String clienteRun = autenticacion == null || autenticacion instanceof AnonymousAuthenticationToken
                ? null : autenticacion.getName();
        return PedidoResponse.desde(servicio.crear(solicitud, clienteRun));
    }

    private void verificarAcceso(Pedido pedido, Authentication autenticacion) {
        if (!esAdministrador(autenticacion)
                && (pedido.getClienteRun() == null || !pedido.getClienteRun().equals(autenticacion.getName()))) {
            throw new AccesoDenegadoException("No puedes consultar esta boleta");
        }
    }

    private boolean esAdministrador(Authentication autenticacion) {
        return autenticacion.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }
}
