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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cl.duoc.skyrimemporium.dto.LineaPedido;
import cl.duoc.skyrimemporium.dto.PedidoRequest;
import cl.duoc.skyrimemporium.dto.AuthResponse;
import cl.duoc.skyrimemporium.dto.LoginRequest;
import cl.duoc.skyrimemporium.dto.CategoriaRequest;
import cl.duoc.skyrimemporium.dto.PerfilRequest;
import cl.duoc.skyrimemporium.dto.UsuarioRequest;
import cl.duoc.skyrimemporium.modelo.Producto;
import cl.duoc.skyrimemporium.repositorio.ProductoRepository;
import cl.duoc.skyrimemporium.servicio.AuthService;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        mockMvc.perform(get("/api/v1/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(558)));

        mockMvc.perform(get("/api/v1/productos").param("gremio", "alquimia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(129));
    }

    @Test
    void swaggerMuestraElEjemploCorregidoDeProducto() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode documento = objectMapper.readTree(resultado.getResponse().getContentAsString());
        JsonNode propiedades = documento.path("components").path("schemas")
                .path("Producto").path("properties");
        JsonNode ejemplo = documento.path("paths").path("/api/v1/productos")
                .path("post").path("requestBody").path("content").path("application/json")
                .path("examples").path("Producto de alquimia").path("value");

        assertTrue(propiedades.has("categoriaSeccion"));
        assertFalse(propiedades.has("categoria"));
        assertTrue(propiedades.path("discountPercent").path("readOnly").asBoolean());
        assertEquals("Pociones", ejemplo.path("categoriaSeccion").asText());
        assertEquals("assets/img/Alquimia/Pociones/Curar_enfermedad.png", ejemplo.path("image").asText());
        assertFalse(ejemplo.has("categoria"));
        assertFalse(ejemplo.has("discountPercent"));
    }

    @Test
    void productoPermiteCrearActualizarYEliminar() throws Exception {
        String id = "crud::" + UUID.randomUUID();
        Producto producto = new Producto(id, "Producto CRUD", 100, "Descripción",
                "assets/img/test.png", 4, "alquimia", "Pociones", 1);
        ObjectNode solicitud = objectMapper.valueToTree(producto);
        solicitud.put("categoria", "alquimia");
        mockMvc.perform(post("/api/v1/productos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Producto CRUD"))
                .andExpect(jsonPath("$.categoriaSeccion").value("Pociones"))
                .andExpect(jsonPath("$.categoria").doesNotExist());

        Producto actualizado = new Producto(id, "Producto actualizado", 120, "Descripción actualizada",
                "assets/img/test.png", 2, "alquimia", "Pociones", 1);
        mockMvc.perform(put("/api/v1/productos/{id}", id)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actualizado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(120));

        mockMvc.perform(delete("/api/v1/productos/{id}", id)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/productos/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void pedidoCalculaPrecioEnServidorYDescuentaStock() throws Exception {
        PedidoRequest solicitud = new PedidoRequest(
                List.of(new LineaPedido(productoId, 2)), "Cliente de prueba", "cliente@ejemplo.cl");
        String tokenCliente = authService.iniciarSesion(
                new LoginRequest("ejemplo@gmail.com", "123456")).respuesta().accessToken();

        MvcResult resultado = mockMvc.perform(post("/api/v1/pedidos")
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

        mockMvc.perform(get("/api/v1/productos/{id}", productoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(1));

        mockMvc.perform(get("/api/v1/pedidos/usuario/{run}", "0000000K")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
        mockMvc.perform(get("/api/v1/pedidos/{id}", pedidoId)
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].total").value(84));
        mockMvc.perform(get("/api/v1/pedidos"))
                .andExpect(status().isUnauthorized());
        String tokenVendedor = authService.iniciarSesion(
                new LoginRequest("vendedor@duoc.cl", "123456")).respuesta().accessToken();
        mockMvc.perform(get("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + tokenVendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pedidoId));
        mockMvc.perform(get("/api/v1/pedidos/{id}", pedidoId)
                        .header("Authorization", "Bearer " + tokenVendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pedidoId));
        mockMvc.perform(get("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].items.length()").value(1));
    }

    @Test
    void ofertaSeExponeEnCatalogoYSeAplicaEnElPedido() throws Exception {
        String ofertaId = "pruebas::pocion-salud";
        productos.save(new Producto(ofertaId, "Poción en oferta", 101, "Descripción",
                "assets/img/test.png", 4, "pruebas", "Pruebas", 1));

        mockMvc.perform(get("/api/v1/productos/{id}", ofertaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountPercent").value(20));

        String tokenCliente = authService.iniciarSesion(
                new LoginRequest("ejemplo@gmail.com", "123456")).respuesta().accessToken();
        PedidoRequest solicitud = new PedidoRequest(
                List.of(new LineaPedido(ofertaId, 2)), "Cliente de prueba", "cliente@ejemplo.cl");
        mockMvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(162))
                .andExpect(jsonPath("$.items[0].precio").value(81))
                .andExpect(jsonPath("$.items[0].total").value(162));
    }

    @Test
    void pedidoSinStockResponde409SinDescontarUnidades() throws Exception {
        PedidoRequest solicitud = new PedidoRequest(
                List.of(new LineaPedido(productoId, 4)), null, null);

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isUnauthorized());

        String tokenCliente = authService.iniciarSesion(
                new LoginRequest("ejemplo@gmail.com", "123456")).respuesta().accessToken();
        mockMvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("quedan 3 unidades")));

        mockMvc.perform(get("/api/v1/productos/{id}", productoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(3));
    }

    @Test
    void gestionesAdminEstanProtegidasYExponenReportes() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isUnauthorized());

        String tokenVendedor = authService.iniciarSesion(
                new LoginRequest("vendedor@duoc.cl", "123456")).respuesta().accessToken();
        mockMvc.perform(get("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + tokenVendedor))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contrasenaHash").doesNotExist());
        mockMvc.perform(get("/api/v1/admin/reportes")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.masVendidos").isArray())
                .andExpect(jsonPath("$.ordenesRecientes").isArray());
    }

    @Test
    void categoriasSePuedenMantenerYNoEliminarSiTienenProductos() throws Exception {
        CategoriaRequest nueva = new CategoriaRequest("alquimia", "Temporal");
        String id = mockMvc.perform(post("/api/v1/categorias")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nueva)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String categoriaId = objectMapper.readTree(id).get("id").asText();

        Producto producto = new Producto("categoria::" + UUID.randomUUID(), "Producto categoría",
                10, "Descripción", "assets/img/test.png", 4, "alquimia", "Temporal", 1);
        mockMvc.perform(post("/api/v1/productos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(producto)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/categorias/{id}", categoriaId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest());

        CategoriaRequest renombrada = new CategoriaRequest("alquimia", "Temporal renombrada");
        mockMvc.perform(put("/api/v1/categorias/{id}", categoriaId)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(renombrada)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/productos/{id}", producto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoriaSeccion").value("Temporal renombrada"))
                .andExpect(jsonPath("$.categoria").doesNotExist());
        mockMvc.perform(delete("/api/v1/productos/{id}", producto.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/categorias/{id}", categoriaId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());
    }

    @Test
    void registroPublicoNoPuedeAsignarseRolPrivilegiado() throws Exception {
        UsuarioRequest nuevo = new UsuarioRequest("12345678-5", "Prueba", "Cliente",
                "registro@ejemplo.cl", "clave123", null, "Administrador",
                "Metropolitana de Santiago", "Santiago", "Calle 123");
        mockMvc.perform(post("/api/v1/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("Cliente"));
    }

    @Test
    void administradorPuedeCrearEditarYEliminarUsuarios() throws Exception {
        UsuarioRequest nuevo = new UsuarioRequest("12345678-5", "Usuario", "Prueba",
                "usuario@ejemplo.cl", "clave123", null, "Vendedor", null, null, null);
        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("Vendedor"))
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist());

        UsuarioRequest actualizado = new UsuarioRequest("12345678-5", "Usuario editado", "Prueba",
                "usuario@ejemplo.cl", null, null, "Cliente", null, null, null);
        mockMvc.perform(put("/api/v1/usuarios/{run}", "123456785")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actualizado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Usuario editado"))
                .andExpect(jsonPath("$.tipo").value("Cliente"));

        mockMvc.perform(delete("/api/v1/usuarios/{run}", "123456785")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/usuarios/{run}", "123456785")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    void loginEntregaBearerTokenYLogoutLoRevoca() throws Exception {
        LoginRequest login = new LoginRequest("vale.rodrigueza@duoc.cl", "1234567");
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.usuario.tipo").value("Administrador"))
                .andReturn();
        String token = objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("vale.rodrigueza@duoc.cl"));
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token)
                        .cookie(new Cookie("skyrim_refresh", cookieToken(resultado))))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenEsJwtYRefreshRotaElToken() throws Exception {
        LoginRequest login = new LoginRequest("vale.rodrigueza@duoc.cl", "1234567");
        MvcResult inicio = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn();

        String accessOriginal = objectMapper.readTree(inicio.getResponse().getContentAsString())
                .get("accessToken").asText();
        String refreshOriginal = cookieToken(inicio);
        org.junit.jupiter.api.Assertions.assertEquals(3, accessOriginal.split("\\.").length);

        MvcResult renovacion = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("skyrim_refresh", refreshOriginal)))
                .andExpect(status().isOk())
                .andReturn();
        String accessRenovado = objectMapper.readTree(renovacion.getResponse().getContentAsString())
                .get("accessToken").asText();
        org.junit.jupiter.api.Assertions.assertNotEquals(accessOriginal, accessRenovado);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("skyrim_refresh", refreshOriginal)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerDocumentaBearerEnLaConfirmacionDePedidos() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/v1/pedidos'].post.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.components.schemas.UsuarioRequest.properties.run.example").value("12345678-5"))
                .andExpect(jsonPath("$.components.schemas.UsuarioRequest.properties.nombre.example").value("Juan"))
                .andExpect(jsonPath("$.components.schemas.LineaPedido.properties.productoId.example")
                        .value("curar-enfermedad"))
                .andExpect(jsonPath("$.components.schemas.AuthResponse.properties.tokenType.example").value("Bearer"))
                .andExpect(jsonPath("$.components.schemas.CategoriaResponse.properties.titulo.example").value("Pociones"));
    }

    @Test
    void loginRechazaFormularioSinCorreo() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contrasena\":\"1234567\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.correo").exists());
    }

    @Test
    void loginRechazaCredencialesInvalidas() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("ejemplo@gmail.com", "incorrecta"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje", containsString("Correo o contras")));
    }

    @Test
    void usuarioPuedeConsultarYActualizarSuPerfil() throws Exception {
        String token = authService.iniciarSesion(
                new LoginRequest("ejemplo@gmail.com", "123456")).respuesta().accessToken();

        mockMvc.perform(get("/api/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ejemplo@gmail.com"));

        PerfilRequest perfil = new PerfilRequest("Nombre editado", "Apellido editado",
                "ejemplo@gmail.com");
        mockMvc.perform(put("/api/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(perfil)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nombre editado"))
                .andExpect(jsonPath("$.correo").value("ejemplo@gmail.com"));
    }

    @Test
    void catalogoFiltraPorGremioYCategoria() throws Exception {
        mockMvc.perform(get("/api/v1/productos").param("gremio", "alquimia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].gremio").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.equalTo("alquimia"))));

        mockMvc.perform(get("/api/v1/productos")
                        .param("gremio", "alquimia")
                        .param("categoria", "Pociones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].categoriaSeccion").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.equalTo("Pociones"))));
    }

    @Test
    void catalogoDevuelveVacioParaGremioInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/productos").param("gremio", "gremio-inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void productoInexistenteResponde404() throws Exception {
        mockMvc.perform(get("/api/v1/productos/{id}", "producto-inexistente"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listadoDeStockCriticoEstaProtegidoParaAdministrador() throws Exception {
        String tokenVendedor = authService.iniciarSesion(
                new LoginRequest("vendedor@duoc.cl", "123456")).respuesta().accessToken();
        mockMvc.perform(get("/api/v1/productos/stock-critico")
                        .header("Authorization", "Bearer " + tokenVendedor))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/productos/stock-critico")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void vendedorNoPuedeCrearEditarNiEliminarProductos() throws Exception {
        String tokenVendedor = authService.iniciarSesion(
                new LoginRequest("vendedor@duoc.cl", "123456")).respuesta().accessToken();
        Producto producto = new Producto("vendedor::bloqueado", "Bloqueado", 100,
                "Descripción", "assets/test.png", 2, "alquimia", "Pociones", 1);

        mockMvc.perform(post("/api/v1/productos")
                        .header("Authorization", "Bearer " + tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(producto)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/productos/{id}", productoId)
                        .header("Authorization", "Bearer " + tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(producto)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/productos/{id}", productoId)
                        .header("Authorization", "Bearer " + tokenVendedor))
                .andExpect(status().isForbidden());
    }

    @Test
    void pedidoNoPermiteQueUnClienteConsulteOtraCuenta() throws Exception {
        String tokenCliente = authService.iniciarSesion(
                new LoginRequest("ejemplo@gmail.com", "123456")).respuesta().accessToken();
        mockMvc.perform(get("/api/v1/pedidos/usuario/{run}", "123456789")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void pedidoRechazaMasUnidadesQueElStockDisponible() throws Exception {
        String tokenCliente = authService.iniciarSesion(
                new LoginRequest("ejemplo@gmail.com", "123456")).respuesta().accessToken();
        PedidoRequest solicitud = new PedidoRequest(
                List.of(new LineaPedido(productoId, 4)), "Cliente", "cliente@ejemplo.cl");
        mockMvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("quedan 3 unidades")));
    }

    @Test
    void pedidoConfirmadoNoAceptaPreciosCalculadosPorElCliente() throws Exception {
        String tokenCliente = authService.iniciarSesion(
                new LoginRequest("ejemplo@gmail.com", "123456")).respuesta().accessToken();
        String cuerpo = "{\"lineas\":[{\"productoId\":\"" + productoId
                + "\",\"cantidad\":1}],\"cliente\":\"Cliente\",\"correo\":\"cliente@ejemplo.cl\","
                + "\"total\":1,\"items\":[{\"precio\":1}]}";
        mockMvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(42));
    }

    @Test
    void categoriasPublicasSePuedenListar() throws Exception {
        mockMvc.perform(get("/api/v1/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void categoriaDuplicadaResponde409() throws Exception {
        CategoriaRequest categoria = new CategoriaRequest("alquimia", "Pociones");
        mockMvc.perform(post("/api/v1/categorias")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoria)))
                .andExpect(status().isConflict());
    }

    @Test
    void clienteNoPuedeMantenerCategorias() throws Exception {
        String tokenCliente = authService.iniciarSesion(
                new LoginRequest("ejemplo@gmail.com", "123456")).respuesta().accessToken();
        CategoriaRequest categoria = new CategoriaRequest("alquimia", "Temporal cliente");
        mockMvc.perform(post("/api/v1/categorias")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoria)))
                .andExpect(status().isForbidden());
    }

    @Test
    void registroPublicoRechazaDatosIncompletos() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Juan\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos").exists());
    }

    @Test
    void perfilPublicoNoPuedeAsignarRolAdministrador() throws Exception {
        UsuarioRequest nuevo = new UsuarioRequest("22345678-2", "Prueba", "Cliente",
                "registro-publico@ejemplo.cl", "clave123", null, "Administrador",
                "Metropolitana de Santiago", "Santiago", "Calle 123");
        mockMvc.perform(post("/api/v1/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("Cliente"));
    }

    @Test
    void vendedorNoPuedeCrearUsuarios() throws Exception {
        String tokenVendedor = authService.iniciarSesion(
                new LoginRequest("vendedor@duoc.cl", "123456")).respuesta().accessToken();
        UsuarioRequest nuevo = new UsuarioRequest("12345678-5", "Usuario", "Prueba",
                "vendedor-crea@ejemplo.cl", "clave123", null, "Cliente", null, null, null);
        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevo)))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorPuedeConsultarElResumen() throws Exception {
        mockMvc.perform(get("/api/v1/admin/resumen")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordenes").isNumber())
                .andExpect(jsonPath("$.productos").isNumber())
                .andExpect(jsonPath("$.usuarios").isNumber());
    }

    @Test
    void clienteNoPuedeConsultarElResumenAdministrativo() throws Exception {
        String tokenCliente = authService.iniciarSesion(
                new LoginRequest("ejemplo@gmail.com", "123456")).respuesta().accessToken();
        mockMvc.perform(get("/api/v1/admin/resumen")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginSinAccesoDebeResponder401EnPerfil() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeLogoutYaNoPermiteAccederAlPerfil() throws Exception {
        LoginRequest login = new LoginRequest("vale.rodrigueza@duoc.cl", "1234567");
        MvcResult inicio = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(inicio.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token)
                        .cookie(new Cookie("skyrim_refresh", cookieToken(inicio))))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshSinCookieDebeResponder401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    private String cookieToken(MvcResult resultado) {
        String setCookie = resultado.getResponse().getHeader("Set-Cookie");
        return setCookie.substring("skyrim_refresh=".length(), setCookie.indexOf(';'));
    }
}
