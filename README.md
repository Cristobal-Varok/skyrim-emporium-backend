# Skyrim Emporium API

API REST construida con Spring Boot 3, Java 21, Maven, Spring Data JPA y MySQL/MariaDB. Incluye catálogo, checkout, cuentas con roles y operaciones administrativas.

## Ejecutar

Requiere Java 21 y MySQL/MariaDB iniciado en XAMPP. La base `skyrimemporium` debe existir y estar cargada con el esquema de `database/skyrimemporium.sql`. Ese script elimina y vuelve a crear la base: no lo ejecutes sobre datos que quieras conservar.

`JWT_SECRET` es una variable de entorno de Windows, no una carpeta ni un archivo del proyecto. El script `run-local.ps1` la genera y guarda automáticamente en las variables de entorno de tu usuario solo la primera vez; no guarda la clave en el repositorio.

```powershell
.\run-local.ps1
```

La API queda disponible en `http://localhost:8081`. Desde la carpeta `backend`, ejecuta `.\run-local.ps1` cada vez que quieras iniciar el backend. Los tokens de acceso se generan automáticamente al iniciar sesión. Las sesiones de renovación se guardan en memoria, así que hay que iniciar sesión de nuevo al reiniciar el backend. Para producción, usa HTTPS y configura `security.jwt.refresh-cookie-secure=true`.

Para ejecutar las pruebas:

```powershell
.\mvnw.cmd test
```

## Autenticación y roles

Las contraseñas se guardan con BCrypt. Inicia sesión en `POST /api/v1/auth/login` con:

```json
{ "correo": "vale.rodrigueza@duoc.cl", "contrasena": "1234567" }
```

La respuesta incluye `accessToken`, `tokenType: "Bearer"`, la fecha de expiración y los datos públicos del usuario. En las siguientes solicitudes privadas envía:

```http
Authorization: Bearer <accessToken>
```

El access token es un JWT firmado que dura 15 minutos. El refresh JWT se entrega en una cookie `HttpOnly`, dura hasta 8 horas y rota al llamar a `POST /api/v1/auth/refresh`. Al cerrar sesión (`POST /api/v1/auth/logout`) se revocan los tokens activos. La lista de refresh y revocaciones vive en memoria, por lo que habrá que volver a iniciar sesión al reiniciar el backend; usuarios, productos y pedidos se guardan en MySQL.

La aplicación mantiene el access token solo en memoria del navegador. Al recargar, solicita un access token nuevo usando la cookie `HttpOnly`; los pedidos requieren una sesión autenticada. El backend obtiene nombre y correo desde la cuenta asociada al token y calcula el total y el stock desde la base de datos.

En una base nueva se crean cuentas de demostración:

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `vale.rodrigueza@duoc.cl` | `1234567` |
| Vendedor | `vendedor@duoc.cl` | `123456` |
| Cliente | `ejemplo@gmail.com` | `123456` |

Son credenciales públicas de demostración, no deben utilizarse en producción. El registro público (`POST /api/v1/usuarios/registro`) siempre crea cuentas de tipo Cliente; solo un administrador autenticado puede gestionar roles.

## Endpoints

### Catálogo y categorías

| Método | Ruta | Acceso | Acción |
|---|---|---|---|
| GET | `/api/v1/productos` | Público | Lista y filtra con `gremio` y/o `categoria` |
| GET | `/api/v1/productos/{id}` | Público | Obtiene un producto |
| POST | `/api/v1/productos` | Administrador | Crea un producto |
| PUT | `/api/v1/productos/{id}` | Administrador | Actualiza producto, precio y stock |
| DELETE | `/api/v1/productos/{id}` | Administrador | Elimina un producto |
| GET | `/api/v1/productos/stock-critico` | Administrador | Lista productos bajo su umbral de stock |
| GET | `/api/v1/categorias` | Público | Lista categorías con conteo de productos |
| POST | `/api/v1/categorias` | Administrador | Crea una categoría |
| PUT | `/api/v1/categorias/{id}` | Administrador | Renombra una categoría y actualiza sus productos |
| DELETE | `/api/v1/categorias/{id}` | Administrador | Elimina categorías vacías |

Los productos conservan identificadores únicos como `alquimia::curar-enfermedad`; las líneas del carrito también pueden enviar el identificador original y el campo `gremio`.

### Usuarios y sesión

| Método | Ruta | Acceso | Acción |
|---|---|---|---|
| POST | `/api/v1/auth/login` | Público | Inicia sesión |
| GET | `/api/v1/auth/me` | Autenticado | Devuelve la cuenta de la sesión |
| POST | `/api/v1/auth/logout` | Autenticado | Revoca el token actual |
| POST | `/api/v1/usuarios/registro` | Público | Registra un cliente |
| GET | `/api/v1/usuarios` | Administrador | Lista cuentas sin exponer contraseñas |
| GET | `/api/v1/usuarios/{run}` | Administrador | Consulta una cuenta |
| POST | `/api/v1/usuarios` | Administrador | Crea cliente, vendedor o administrador |
| PUT | `/api/v1/usuarios/{run}` | Administrador | Actualiza cuenta y opcionalmente contraseña |
| DELETE | `/api/v1/usuarios/{run}` | Administrador | Elimina una cuenta, salvo la última cuenta admin |
| GET | `/api/v1/usuarios/me` | Autenticado | Consulta el perfil propio |
| PUT | `/api/v1/usuarios/me` | Autenticado | Actualiza nombre, apellidos y correo propios |

### Pedidos, boletas e informes

| Método | Ruta | Acceso | Acción |
|---|---|---|---|
| POST | `/api/v1/pedidos` | Autenticado | Registra compra, calcula total y descuenta stock en una transacción |
| GET | `/api/v1/pedidos` | Administrador o Vendedor | Lista órdenes con detalle de productos |
| GET | `/api/v1/pedidos/{id}` | Administrador, Vendedor o dueño | Consulta boleta |
| GET | `/api/v1/pedidos/usuario/{run}` | Administrador o dueño | Historial de compras |
| GET | `/api/v1/admin/resumen` | Administrador | Totales para el inicio del panel |
| GET | `/api/v1/admin/reportes` | Administrador | Ingresos, órdenes, stock crítico, más vendidos y órdenes recientes |

El checkout debe llamar a `POST /api/v1/pedidos` solo cuando el pago simulado termina correctamente. El servidor no procesa pagos: guarda el pedido confirmado con estado `Pagada`. Cada boleta conserva el precio y nombre de los productos al momento de comprar, además de los datos de despacho.

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

El backend obtiene precio e inventario desde la base de datos, aplica las ofertas configuradas al producto (`discountPercent` en el catálogo) y guarda el precio final en cada boleta. No acepta precios enviados desde el frontend. Si falta stock, responde `409` sin dejar descuentos parciales. Los errores usan `{ "status": 409, "mensaje": "...", "campos": {} }`.

## Documentación y consola

- Swagger UI: `http://localhost:8081/swagger-ui.html` (incluye el esquema Bearer JWT y el botón **Authorize**)
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- phpMyAdmin de XAMPP: `http://localhost/phpmyadmin`

CORS permite Vite en `localhost:5173` y `localhost:5174`.

## Estado de integración

El frontend consume esta API mediante Axios. Configura `VITE_API_URL` con la base versionada; en desarrollo local usa `http://localhost:8081/api/v1`. Los pedidos deben enviar identificador de producto y cantidad, sin confiar en el precio calculado por el navegador. El carrito puede persistir en `localStorage`, pero la autenticación usa el access token en memoria y la cookie de renovación `HttpOnly`. Las credenciales de demostración del frontend y del backend coinciden.

La configuración es para desarrollo local. Antes de publicar el servicio hay que cambiar las credenciales de demostración, usar una base gestionada y HTTPS, y reemplazar el almacenamiento en memoria de tokens por una estrategia adecuada al entorno de despliegue.
