package cl.duoc.skyrimemporium.controlador;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import cl.duoc.skyrimemporium.dto.AdminResumenResponse;
import cl.duoc.skyrimemporium.dto.ReporteAdminResponse;
import cl.duoc.skyrimemporium.repositorio.ProductoRepository;
import cl.duoc.skyrimemporium.repositorio.UsuarioRepository;
import cl.duoc.skyrimemporium.servicio.ReporteAdminService;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final ReporteAdminService reportes;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;

    public AdminController(ReporteAdminService reportes, ProductoRepository productos, UsuarioRepository usuarios) {
        this.reportes = reportes;
        this.productos = productos;
        this.usuarios = usuarios;
    }

    @GetMapping("/reportes")
    public ReporteAdminResponse reportes() {
        return reportes.obtener();
    }

    @GetMapping("/resumen")
    public AdminResumenResponse resumen() {
        ReporteAdminResponse reporte = reportes.obtener();
        return new AdminResumenResponse(reporte.ordenes(), reporte.ingresos(), productos.count(), usuarios.count());
    }
}
