package cl.duoc.skyrimemporium.servicio;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.swagger.v3.oas.annotations.media.Schema;
import cl.duoc.skyrimemporium.dto.CategoriaRequest;
import cl.duoc.skyrimemporium.error.RecursoDuplicadoException;
import cl.duoc.skyrimemporium.error.RecursoNoEncontradoException;
import cl.duoc.skyrimemporium.modelo.Categoria;
import cl.duoc.skyrimemporium.repositorio.CategoriaRepository;
import cl.duoc.skyrimemporium.repositorio.ProductoRepository;

@Service
public class CategoriaService {

    private static final Set<String> GREMIOS = Set.of(
            "herreria", "alquimia", "bienesGenerales", "ropaFina", "comida", "magia");
    private final CategoriaRepository categorias;
    private final ProductoRepository productos;

    public CategoriaService(CategoriaRepository categorias, ProductoRepository productos) {
        this.categorias = categorias;
        this.productos = productos;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categorias.findAllByOrderByGremioAscTituloAsc().stream()
                .map(this::respuesta).toList();
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest solicitud) {
        String gremio = solicitud.gremio().trim();
        String titulo = solicitud.titulo().trim();
        validarGremio(gremio);
        if (categorias.existsByGremioIgnoreCaseAndTituloIgnoreCase(gremio, titulo)) {
            throw new RecursoDuplicadoException("Ya existe esa categoría en el gremio");
        }
        String id = "custom-" + gremio + "-" + UUID.randomUUID();
        return respuesta(categorias.save(new Categoria(id, gremio, titulo)));
    }

    @Transactional
    public CategoriaResponse actualizar(String id, CategoriaRequest solicitud) {
        Categoria actual = buscar(id);
        if (!actual.getGremio().equalsIgnoreCase(solicitud.gremio().trim())) {
            throw new IllegalArgumentException("No se puede cambiar el gremio de una categoría");
        }
        String titulo = solicitud.titulo().trim();
        if (!actual.getTitulo().equalsIgnoreCase(titulo)
                && categorias.existsByGremioIgnoreCaseAndTituloIgnoreCase(actual.getGremio(), titulo)) {
            throw new RecursoDuplicadoException("Ya existe esa categoría en el gremio");
        }
        String anterior = actual.getTitulo();
        actual.setTitulo(titulo);
        productos.findAllByGremioIgnoreCaseAndCategoriaSeccionIgnoreCase(actual.getGremio(), anterior)
                .forEach(p -> p.setCategoriaSeccion(titulo));
        return respuesta(actual);
    }

    @Transactional
    public void eliminar(String id) {
        Categoria categoria = buscar(id);
        boolean tieneProductos = productos.existsByGremioIgnoreCaseAndCategoriaSeccionIgnoreCase(
                categoria.getGremio(), categoria.getTitulo());
        if (tieneProductos) {
            throw new IllegalArgumentException("No se puede eliminar una categoría que contiene productos");
        }
        categorias.delete(categoria);
    }

    private Categoria buscar(String id) {
        return categorias.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoría " + id));
    }

    private CategoriaResponse respuesta(Categoria categoria) {
        long cantidad = productos.countByGremioIgnoreCaseAndCategoriaSeccionIgnoreCase(
                categoria.getGremio(), categoria.getTitulo());
        return new CategoriaResponse(categoria.getId(), categoria.getGremio(), categoria.getTitulo(), cantidad);
    }

    private void validarGremio(String gremio) {
        if (!GREMIOS.contains(gremio)) {
            throw new IllegalArgumentException("El gremio no es válido");
        }
    }

    public static String idInicial(String gremio, String titulo) {
        String slug = Normalizer.normalize(titulo, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return gremio + "::" + slug;
    }

    public record CategoriaResponse(
            @Schema(example = "alquimia::pociones") String id,
            @Schema(example = "alquimia") String gremio,
            @Schema(example = "Pociones") String titulo,
            @Schema(example = "12") long productos) { }
}
