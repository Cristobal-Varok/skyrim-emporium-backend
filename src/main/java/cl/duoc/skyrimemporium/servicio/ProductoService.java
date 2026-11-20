package cl.duoc.skyrimemporium.servicio;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.skyrimemporium.error.RecursoDuplicadoException;
import cl.duoc.skyrimemporium.error.RecursoNoEncontradoException;
import cl.duoc.skyrimemporium.modelo.Producto;
import cl.duoc.skyrimemporium.repositorio.CategoriaRepository;
import cl.duoc.skyrimemporium.repositorio.ProductoRepository;

@Service
public class ProductoService {

    private final ProductoRepository repositorio;
    private final CategoriaRepository categorias;

    public ProductoService(ProductoRepository repositorio, CategoriaRepository categorias) {
        this.repositorio = repositorio;
        this.categorias = categorias;
    }

    @Transactional(readOnly = true)
    public List<Producto> listar(String gremio, String categoria) {
        return repositorio.findAll(Sort.by("id").ascending()).stream()
                .filter(producto -> gremio == null || gremio.isBlank() || producto.getGremio().equalsIgnoreCase(gremio))
                .filter(producto -> categoria == null || categoria.isBlank()
                        || producto.getCategoriaSeccion().equalsIgnoreCase(categoria))
                .toList();
    }

    @Transactional(readOnly = true)
    public Producto buscar(String id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + id));
    }

    @Transactional
    public Producto crear(Producto producto) {
        if (repositorio.existsById(producto.getId())) {
            throw new RecursoDuplicadoException("Ya existe un producto con el identificador " + producto.getId());
        }
        validarCategoria(producto);
        if (producto.getCodigo() == null || producto.getCodigo().isBlank()) {
            producto.setCodigo(producto.getId());
        }
        return repositorio.save(producto);
    }

    @Transactional
    public Producto actualizar(String id, Producto datos) {
        Producto actual = buscar(id);
        validarCategoria(datos);
        if (!actual.getCodigo().equalsIgnoreCase(datos.getCodigo())
                && repositorio.existsByCodigoIgnoreCaseAndIdNot(datos.getCodigo(), id)) {
            throw new RecursoDuplicadoException("Ya existe un producto con el código " + datos.getCodigo());
        }
        actual.setCodigo(datos.getCodigo());
        actual.setName(datos.getName());
        actual.setPrice(datos.getPrice());
        actual.setDescription(datos.getDescription());
        actual.setImage(datos.getImage());
        actual.setStock(datos.getStock());
        actual.setGremio(datos.getGremio());
        actual.setCategoriaSeccion(datos.getCategoriaSeccion());
        actual.setStockCritico(datos.getStockCritico());
        return repositorio.save(actual);
    }

    @Transactional(readOnly = true)
    public List<Producto> stockCritico() {
        return repositorio.findAll(Sort.by("stock").ascending()).stream()
                .filter(p -> p.getStock() <= p.getStockCritico())
                .toList();
    }

    private void validarCategoria(Producto producto) {
        boolean existe = categorias.existsByGremioIgnoreCaseAndTituloIgnoreCase(
                producto.getGremio(), producto.getCategoriaSeccion());
        if (!existe) {
            throw new RecursoNoEncontradoException("No existe la categoría para el gremio indicado");
        }
    }

    @Transactional
    public void eliminar(String id) {
        repositorio.delete(buscar(id));
    }
}
