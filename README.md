# Skyrim Emporium API

API REST construida con Spring Boot 3, Java 21, Maven, Spring Data JPA y H2. Incluye catálogo, checkout, cuentas con roles y las operaciones usadas por el panel administrativo.

## Ejecutar

Requiere Java 21. Abre una terminal PowerShell en `backend` y ejecuta:

```powershell
.\mvnw.cmd spring-boot:run
```

La API queda disponible en `http://localhost:8080`. El Maven Wrapper descarga Maven y dependencias si es necesario. H2 guarda sus datos en `backend/data/` y los conserva al reiniciar; esa carpeta está excluida de Git. Para empezar con una base vacía, detén el servidor y elimina solamente los archivos de esa base local.

Para ejecutar las pruebas:

```powershell
.\mvnw.cmd test
```

## Autenticación y roles

Las contraseñas se guardan con BCrypt. Inicia sesión en `POST /api/auth/login` con:

```json
{ "correo": "vale.rodrigueza@duoc.cl", "contrasena": "1234567" }
```

La respuesta incluye `accessToken`, `tokenType: "Bearer"`, la fecha de expiración y los datos públicos del usuario. En las siguientes solicitudes privadas envía:

```http
Authorization: Bearer <accessToken>
```

El token dura 8 horas y se revoca al cerrar sesión (`POST /api/auth/logout`). Por ahora los tokens viven en memoria y dejan de servir al reiniciar el backend; las cuentas, productos y pedidos sí persisten en H2.

En una base local nueva se crean cuentas de demostración:

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `vale.rodrigueza@duoc.cl` | `1234567` |
| Vendedor | `vendedor@duoc.cl` | `123456` |
| Cliente | `ejemplo@gmail.com` | `123456` |

Son credenciales públicas de demostración, no deben utilizarse en producción. El registro público (`POST /api/usuarios/registro`) siempre crea cuentas de tipo Cliente; solo un administrador autenticado puede gestionar roles.

## Endpoints

### Catálogo y categorías

| Método | Ruta | Acceso | Acción |
|---|---|---|---|
| GET | `/api/productos` | Público | Lista y filtra con `gremio` y/o `categoria` |
| GET | `/api/productos/{id}` | Público | Obtiene un producto |
| POST | `/api/productos` | Administrador | Crea un producto |
| PUT | `/api/productos/{id}` | Administrador | Actualiza producto, precio y stock |
| DELETE | `/api/productos/{id}` | Administrador | Elimina un producto |
| GET | `/api/productos/stock-critico` | Administrador | Lista productos bajo su umbral de stock |
| GET | `/api/categorias` | Público | Lista categorías con conteo de productos |
| POST | `/api/categorias` | Administrador | Crea una categoría |
| PUT | `/api/categorias/{id}` | Administrador | Renombra una categoría y actualiza sus productos |
| DELETE | `/api/categorias/{id}` | Administrador | Elimina categorías vacías |

Los productos conservan identificadores únicos como `alquimia::curar-enfermedad`; las líneas del carrito también pueden enviar el identificador original y el campo `gremio`.

### Usuarios y sesión

| Método | Ruta | Acceso | Acción |
|---|---|---|---|
| POST | `/api/auth/login` | Público | Inicia sesión |
| GET | `/api/auth/me` | Autenticado | Devuelve la cuenta de la sesión |
| POST | `/api/auth/logout` | Autenticado | Revoca el token actual |
| POST | `/api/usuarios/registro` | Público | Registra un cliente |
| GET | `/api/usuarios` | Administrador | Lista cuentas sin exponer contraseñas |
| GET | `/api/usuarios/{run}` | Administrador | Consulta una cuenta |
| POST | `/api/usuarios` | Administrador | Crea cliente, vendedor o administrador |
| PUT | `/api/usuarios/{run}` | Administrador | Actualiza cuenta y opcionalmente contraseña |
| DELETE | `/api/usuarios/{run}` | Administrador | Elimina una cuenta, salvo la última cuenta admin |
| GET | `/api/usuarios/me` | Autenticado | Consulta el perfil propio |
| PUT | `/api/usuarios/me` | Autenticado | Actualiza nombre, apellidos y correo propios |

### Pedidos, boletas e informes

| Método | Ruta | Acceso | Acción |
|---|---|---|---|
| POST | `/api/pedidos` | Público o autenticado | Registra compra, calcula total y descuenta stock en una transacción |
| GET | `/api/pedidos` | Administrador | Lista órdenes con detalle de productos |
| GET | `/api/pedidos/{id}` | Administrador o dueño | Consulta boleta |
| GET | `/api/pedidos/usuario/{run}` | Administrador o dueño | Historial de compras |
| GET | `/api/admin/resumen` | Administrador | Totales para el inicio del panel |
| GET | `/api/admin/reportes` | Administrador | Ingresos, órdenes, stock crítico, más vendidos y órdenes recientes |

El checkout debe llamar a `POST /api/pedidos` solo cuando el pago simulado termina correctamente. El servidor no procesa pagos: guarda el pedido confirmado con estado `Pagada`. Cada boleta conserva el precio y nombre de los productos al momento de comprar, además de los datos de despacho.

Ejemplo mínimo de pedido:

```json
{
  "cliente": "Nombre Apellido",
  "correo": "cliente@ejemplo.cl",
  "lineas": [
    { "productoId": "curar-enfermedad", "gremio": "alquimia", "cantidad": 2 }
  ],
  "calle": "Avenida Skyrim 123",
  "region": "Metropolitana de Santiago",
  "comuna": "Santiago"
}
```

El backend obtiene precio e inventario desde la base de datos. Si falta stock, responde `409` sin dejar descuentos parciales. Los errores usan `{ "status": 409, "mensaje": "...", "campos": {} }`.

## Documentación y consola

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Consola H2: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./data/skyrimemporium`, usuario `sa`, contraseña vacía)

CORS permite Vite en `localhost:5173` y `localhost:5174`.

## Estado de integración

El backend ya ofrece las operaciones y contratos para el catálogo, checkout y administración descritos en el README del frontend. **El frontend todavía no está conectado**: mientras no se actualice su `AuthContext`, carrito y mantenedores para llamar a esta API, seguirá leyendo y guardando datos en `localStorage`. Las credenciales de demostración del frontend y del backend coinciden.

La configuración es para desarrollo local. Antes de publicar el servicio hay que cambiar las credenciales de demostración, usar una base gestionada y HTTPS, y reemplazar el almacenamiento en memoria de tokens por una estrategia adecuada al entorno de despliegue.
