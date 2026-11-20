package cl.duoc.skyrimemporium.servicio;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.skyrimemporium.dto.PerfilRequest;
import cl.duoc.skyrimemporium.dto.UsuarioRequest;
import cl.duoc.skyrimemporium.error.AccesoDenegadoException;
import cl.duoc.skyrimemporium.error.RecursoDuplicadoException;
import cl.duoc.skyrimemporium.error.RecursoNoEncontradoException;
import cl.duoc.skyrimemporium.modelo.Usuario;
import cl.duoc.skyrimemporium.repositorio.UsuarioRepository;

@Service
public class UsuarioService {

    private static final Set<String> TIPOS = Set.of("Cliente", "Vendedor", "Administrador");
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.findAll().stream()
                .sorted((a, b) -> a.getApellidos().compareToIgnoreCase(b.getApellidos()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Usuario buscar(String run) {
        return usuarios.findById(normalizarRun(run))
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + run));
    }

    @Transactional
    public Usuario crear(UsuarioRequest datos, boolean registroPublico) {
        String run = normalizarRun(datos.run());
        String correo = datos.correo().trim().toLowerCase(Locale.ROOT);
        validarRun(run);
        if (usuarios.existsById(run) || usuarios.existsByCorreoIgnoreCase(correo)) {
            throw new RecursoDuplicadoException("El RUN o correo ya está registrado");
        }
        validarTipo(registroPublico ? "Cliente" : datos.tipo());
        if (datos.contrasena() == null || datos.contrasena().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        return usuarios.save(new Usuario(run, datos.nombre().trim(), datos.apellidos().trim(), correo,
                passwordEncoder.encode(datos.contrasena()), datos.fechaNacimiento(),
                registroPublico ? "Cliente" : datos.tipo(), datos.region(), datos.comuna(),
                limpiar(datos.direccion())));
    }

    @Transactional
    public Usuario actualizar(String run, UsuarioRequest datos) {
        Usuario actual = buscar(run);
        String correo = datos.correo().trim().toLowerCase(Locale.ROOT);
        if (!actual.getCorreo().equalsIgnoreCase(correo) && usuarios.existsByCorreoIgnoreCase(correo)) {
            throw new RecursoDuplicadoException("El correo ya está registrado");
        }
        validarTipo(datos.tipo());
        if ("Administrador".equals(actual.getTipo()) && !"Administrador".equals(datos.tipo())
                && usuarios.countByTipoIgnoreCase("Administrador") <= 1) {
            throw new AccesoDenegadoException("Debe quedar al menos un administrador");
        }
        actual.setNombre(datos.nombre().trim());
        actual.setApellidos(datos.apellidos().trim());
        actual.setCorreo(correo);
        actual.setFechaNacimiento(datos.fechaNacimiento());
        actual.setTipo(datos.tipo());
        actual.setRegion(limpiar(datos.region()));
        actual.setComuna(limpiar(datos.comuna()));
        actual.setDireccion(limpiar(datos.direccion()));
        if (datos.contrasena() != null && !datos.contrasena().isBlank()) {
            actual.setContrasenaHash(passwordEncoder.encode(datos.contrasena()));
        }
        return actual;
    }

    @Transactional
    public Usuario actualizarPerfil(String run, PerfilRequest datos) {
        Usuario actual = buscar(run);
        String correo = datos.correo().trim().toLowerCase(Locale.ROOT);
        if (!actual.getCorreo().equalsIgnoreCase(correo) && usuarios.existsByCorreoIgnoreCase(correo)) {
            throw new RecursoDuplicadoException("El correo ya está registrado");
        }
        actual.setNombre(datos.nombre().trim());
        actual.setApellidos(datos.apellidos().trim());
        actual.setCorreo(correo);
        return actual;
    }

    @Transactional
    public void eliminar(String run, String actorRun) {
        Usuario usuario = buscar(run);
        if (usuario.getRun().equals(actorRun)) {
            throw new AccesoDenegadoException("No puedes eliminar la cuenta con la que tienes la sesión iniciada");
        }
        if ("Administrador".equals(usuario.getTipo())
                && usuarios.countByTipoIgnoreCase("Administrador") <= 1) {
            throw new AccesoDenegadoException("Debe quedar al menos un administrador");
        }
        usuarios.delete(usuario);
    }

    public static String normalizarRun(String run) {
        return run == null ? "" : run.replace(".", "").replace("-", "").trim().toUpperCase(Locale.ROOT);
    }

    private void validarTipo(String tipo) {
        if (tipo == null || !TIPOS.contains(tipo)) {
            throw new IllegalArgumentException("El tipo debe ser Cliente, Vendedor o Administrador");
        }
    }

    private void validarRun(String run) {
        if (!run.matches("\\d{7,8}[0-9K]")) {
            throw new IllegalArgumentException("El RUN no tiene un formato válido");
        }
        String cuerpo = run.substring(0, run.length() - 1);
        int suma = 0;
        int multiplicador = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.digit(cuerpo.charAt(i), 10) * multiplicador;
            multiplicador = multiplicador == 7 ? 2 : multiplicador + 1;
        }
        int digito = 11 - suma % 11;
        String verificador = digito == 11 ? "0" : digito == 10 ? "K" : Integer.toString(digito);
        if (!verificador.equals(run.substring(run.length() - 1))) {
            throw new IllegalArgumentException("El dígito verificador del RUN no es válido");
        }
    }

    private String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
