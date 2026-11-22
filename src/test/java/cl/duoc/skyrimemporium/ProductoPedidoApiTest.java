package cl.duoc.skyrimemporium;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.TestPropertySource;
import jakarta.servlet.http.Cookie;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.duoc.skyrimemporium.dto.LineaPedido;
import cl.duoc.skyrimemporium.dto.PedidoRequest;
import cl.duoc.skyrimemporium.dto.AuthResponse;
import cl.duoc.skyrimemporium.dto.LoginRequest;
import cl.duoc.skyrimemporium.dto.CategoriaRequest;
import cl.duoc.skyrimemporium.dto.UsuarioRequest;
import cl.duoc.skyrimemporium.modelo.Producto;
import cl.duoc.skyrimemporium.repositorio.ProductoRepository;
import cl.duoc.skyrimemporium.servicio.AuthService;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:skyrimemporium-tests;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="
})
class ProductoPedidoApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductoRepository productos;

    @Autowired
    private AuthService authService;

    private String productoId;
    private String tokenAdmin;

    @BeforeEach
    void crearProductoDePrueba() {
        AuthResponse sesion = authService.iniciarSesion(
                new LoginRequest("vale.rodrigueza@duoc.cl", "1234567")).respuesta();
        tokenAdmin = sesion.accessToken();
        productoId = "pruebas::" + UUID.randomUUID();
        productos.save(new Producto(productoId, "Producto de prueba", 42, "Descripción de prueba",
                "assets/img/test.png", 3, "pruebas", "Pruebas", 1));
    }

    @Test
    void catalogoCargaLosProductosDelFrontend() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(558)));

        mockMvc.perform(get("/api/productos").param("gremio", "alquimia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(129));
    }

    @Test
    void productoPermiteCrearActualizarYEliminar() throws Exception {
        String id = "crud::" + UUID.randomUUID();
        Producto producto = new Producto(id, "Producto CRUD", 100, "Descripción",
                "assets/img/test.png", 4, "alquimia", "Pociones", 1);
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(producto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Producto CRUD"));

        Producto actualizado = new Producto(id, "Producto actualizado", 120, "Descripción actualizada",
                "assets/img/test.png", 2, "alquimia", "Pociones", 1);
        mockMvc.perform(put("/api/productos/{id}", id)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actualizado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(120));

        mockMvc.perform(delete("/api/productos/{id}", id)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/productos/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void pedidoCalculaPrecioEnServidorYDescuentaStock() throws Exception {
        PedidoRequest solicitud = new PedidoRequest(
                List.of(new LineaPedido(productoId, 2)), "Cliente de prueba", "cliente@ejemplo.cl");
        String tokenCliente = authService.iniciarSesion(
                new LoginRequest("ejemplo@gmail.com", "123456")).respuesta().accessToken();

        MvcResult resultado = mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unidades").value(2))
                .andExpect(jsonPath("$.total").value(84))
                .andExpect(jsonPath("$.items[0].nombre").value("Producto de prueba"))
                .andExpect(jsonPath("$.items[0].precio").value(42))
                .andExpect(jsonPath("$.estado").value("Pagada"))
                .andExpect(jsonPath("$.clienteRun").value("0000000K"))
                .andReturn();
        long pedidoId = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/productos/{id}", productoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(1));

        mockMvc.perform(get("/api/pedidos/usuario/{run}", "0000000K")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/pedidos/{id}", pedidoId)
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].total").value(84));
        mockMvc.perform(get("/api/pedidos"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/pedidos")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].items.length()").value(1));
    }

    @Test
    void pedidoSinStockResponde409SinDescontarUnidades() throws Exception {
        PedidoRequest solicitud = new PedidoRequest(
                List.of(new LineaPedido(productoId, 4)), null, null);

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isUnauthorized());

        String tokenCliente = authService.iniciarSesion(
                new LoginRequest("ejemplo@gmail.com", "123456")).respuesta().accessToken();
        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("quedan 3 unidades")));

        mockMvc.perform(get("/api/productos/{id}", productoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(3));
    }

    @Test
    void gestionesAdminEstanProtegidasYExponenReportes() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized());

        String tokenVendedor = authService.iniciarSesion(
                new LoginRequest("vendedor@duoc.cl", "123456")).respuesta().accessToken();
        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenVendedor))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contrasenaHash").doesNotExist());
        mockMvc.perform(get("/api/admin/reportes")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.masVendidos").isArray())
                .andExpect(jsonPath("$.ordenesRecientes").isArray());
    }

    @Test
    void categoriasSePuedenMantenerYNoEliminarSiTienenProductos() throws Exception {
        CategoriaRequest nueva = new CategoriaRequest("alquimia", "Temporal");
        String id = mockMvc.perform(post("/api/categorias")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nueva)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String categoriaId = objectMapper.readTree(id).get("id").asText();

        Producto producto = new Producto("categoria::" + UUID.randomUUID(), "Producto categoría",
                10, "Descripción", "assets/img/test.png", 4, "alquimia", "Temporal", 1);
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(producto)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/categorias/{id}", categoriaId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest());

        CategoriaRequest renombrada = new CategoriaRequest("alquimia", "Temporal renombrada");
        mockMvc.perform(put("/api/categorias/{id}", categoriaId)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(renombrada)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/productos/{id}", producto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("Temporal renombrada"));
        mockMvc.perform(delete("/api/productos/{id}", producto.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/categorias/{id}", categoriaId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());
    }

    @Test
    void registroPublicoNoPuedeAsignarseRolPrivilegiado() throws Exception {
        UsuarioRequest nuevo = new UsuarioRequest("12345678-5", "Prueba", "Cliente",
                "registro@ejemplo.cl", "clave123", null, "Administrador",
                "Metropolitana de Santiago", "Santiago", "Calle 123");
        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("Cliente"));
    }

    @Test
    void administradorPuedeCrearEditarYEliminarUsuarios() throws Exception {
        UsuarioRequest nuevo = new UsuarioRequest("12345678-5", "Usuario", "Prueba",
                "usuario@ejemplo.cl", "clave123", null, "Vendedor", null, null, null);
        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("Vendedor"))
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist());

        UsuarioRequest actualizado = new UsuarioRequest("12345678-5", "Usuario editado", "Prueba",
                "usuario@ejemplo.cl", null, null, "Cliente", null, null, null);
        mockMvc.perform(put("/api/usuarios/{run}", "123456785")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actualizado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Usuario editado"))
                .andExpect(jsonPath("$.tipo").value("Cliente"));

        mockMvc.perform(delete("/api/usuarios/{run}", "123456785")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/usuarios/{run}", "123456785")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    void loginEntregaBearerTokenYLogoutLoRevoca() throws Exception {
        LoginRequest login = new LoginRequest("vale.rodrigueza@duoc.cl", "1234567");
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.usuario.tipo").value("Administrador"))
                .andReturn();
        String token = objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("vale.rodrigueza@duoc.cl"));
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token)
                        .cookie(new Cookie("skyrim_refresh", cookieToken(resultado))))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenEsJwtYRefreshRotaElToken() throws Exception {
        LoginRequest login = new LoginRequest("vale.rodrigueza@duoc.cl", "1234567");
        MvcResult inicio = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn();

        String accessOriginal = objectMapper.readTree(inicio.getResponse().getContentAsString())
                .get("accessToken").asText();
        String refreshOriginal = cookieToken(inicio);
        org.junit.jupiter.api.Assertions.assertEquals(3, accessOriginal.split("\\.").length);

        MvcResult renovacion = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("skyrim_refresh", refreshOriginal)))
                .andExpect(status().isOk())
                .andReturn();
        String accessRenovado = objectMapper.readTree(renovacion.getResponse().getContentAsString())
                .get("accessToken").asText();
        org.junit.jupiter.api.Assertions.assertNotEquals(accessOriginal, accessRenovado);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("skyrim_refresh", refreshOriginal)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerDocumentaBearerEnLaConfirmacionDePedidos() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/pedidos'].post.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.components.schemas.UsuarioRequest.properties.run.example").value("12345678-5"))
                .andExpect(jsonPath("$.components.schemas.UsuarioRequest.properties.nombre.example").value("Juan"))
                .andExpect(jsonPath("$.components.schemas.LineaPedido.properties.productoId.example")
                        .value("curar-enfermedad"))
                .andExpect(jsonPath("$.components.schemas.AuthResponse.properties.tokenType.example").value("Bearer"))
                .andExpect(jsonPath("$.components.schemas.CategoriaResponse.properties.titulo.example").value("Pociones"));
    }

    private String cookieToken(MvcResult resultado) {
        String setCookie = resultado.getResponse().getHeader("Set-Cookie");
        return setCookie.substring("skyrim_refresh=".length(), setCookie.indexOf(';'));
    }
}
