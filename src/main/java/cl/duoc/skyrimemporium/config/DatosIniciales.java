package cl.duoc.skyrimemporium.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.duoc.skyrimemporium.modelo.Producto;
import cl.duoc.skyrimemporium.modelo.Categoria;
import cl.duoc.skyrimemporium.modelo.Usuario;
import cl.duoc.skyrimemporium.repositorio.CategoriaRepository;
import cl.duoc.skyrimemporium.repositorio.ProductoRepository;
import cl.duoc.skyrimemporium.repositorio.UsuarioRepository;
import cl.duoc.skyrimemporium.servicio.CategoriaService;

@Component
public class DatosIniciales implements CommandLineRunner {

    private final ProductoRepository repositorio;
    private final CategoriaRepository categorias;
    private final UsuarioRepository usuarios;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

    public DatosIniciales(ProductoRepository repositorio, CategoriaRepository categorias,
            UsuarioRepository usuarios, ObjectMapper objectMapper, PasswordEncoder passwordEncoder) {
        this.repositorio = repositorio;
        this.categorias = categorias;
        this.usuarios = usuarios;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws IOException {
        if (repositorio.count() == 0) {
            ClassPathResource archivo = new ClassPathResource("data/productos.json");
            try (InputStream entrada = archivo.getInputStream()) {
                List<Producto> productos = Arrays.asList(objectMapper.readValue(entrada, Producto[].class));
                repositorio.saveAll(productos);
            }
        }
        repositorio.findAll().stream()
                .map(p -> new Categoria(CategoriaService.idInicial(p.getGremio(), p.getCategoriaSeccion()),
                        p.getGremio(), p.getCategoriaSeccion()))
                .forEach(categoria -> categorias.findById(categoria.getId())
                        .orElseGet(() -> categorias.save(categoria)));
        if (usuarios.count() == 0) crearCuentasDemostracion();
    }

    private void crearCuentasDemostracion() {
        usuarios.saveAll(List.of(
                new Usuario("123456789", "Valeria", "Rodriguez", "vale.rodrigueza@duoc.cl",
                        passwordEncoder.encode("1234567"), null, "Administrador", null, null, null),
                new Usuario("111111111", "Vendedor", "Skyrim", "vendedor@duoc.cl",
                        passwordEncoder.encode("123456"), null, "Vendedor", null, null, null),
                new Usuario("0000000K", "NOMBRE", "APELLIDOS", "ejemplo@gmail.com",
                        passwordEncoder.encode("123456"), null, "Cliente", null, null, null)));
    }
}
