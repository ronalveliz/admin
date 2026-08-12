package Bumerak.administrador.servicios;

import Bumerak.administrador.dto.request.RegistroRequest;
import Bumerak.administrador.entidades.Grupo;
import Bumerak.administrador.entidades.Perfil;
import Bumerak.administrador.entidades.Usuarios;
import Bumerak.administrador.entidades.enums.TipoPerfil;
import Bumerak.administrador.entidades.enums.TipoRol;
import Bumerak.administrador.exception.CustomException;
import Bumerak.administrador.repositorios.GrupoRepository;
import Bumerak.administrador.repositorios.PerfilRepository;
import Bumerak.administrador.repositorios.UsuariosRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UsuarioService {
    private final UsuariosRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final GrupoRepository grupoRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registrar un nuevo usuario administrador (Juan)
     */
    public Usuarios registrarAdministrador(RegistroRequest request) {
        log.info("📝 Registrando nuevo administrador: {}", request.getEmail());

        // Validar que el email no exista
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new CustomException("El email " + request.getEmail() + " ya está registrado");
        }

        // Validar que el rol sea válido para administrador
        if (!request.getRoleName().esAdministrador()) {
            throw new CustomException("El rol " + request.getRoleName() + " no es válido para administrador");
        }

        // Crear el usuario
        Usuarios usuario = Usuarios.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nombre(request.getNombre())
                .rol(request.getRoleName())
                .telefono(request.getTelefono())
                .direccion(request.getDireccion())
                .enabled(true)
                .fechaCreacion(LocalDateTime.now())
                .build();

        Usuarios savedUsuario = usuarioRepository.save(usuario);
        log.info("✅ Usuario creado: {}", savedUsuario.getEmail());

        // Si el usuario tiene grupo, crearlo
        if (request.getNombreGrupo() != null && !request.getNombreGrupo().isEmpty()) {
            crearGrupoParaUsuario(savedUsuario, request);
        }

        // Crear perfil personal automáticamente
        crearPerfilPersonal(savedUsuario);

        return savedUsuario;
    }

    /**
     * Crear grupo para un usuario administrador
     */
    private void crearGrupoParaUsuario(Usuarios administrador, RegistroRequest request) {
        Grupo grupo = Grupo.builder()
                .nombre(request.getNombreGrupo())
                .descripcion(request.getDescripcionGrupo())
                .tipo(administrador.getRol().getTipoGrupoAsociado())
                .administrador(administrador)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();

        Grupo savedGrupo = grupoRepository.save(grupo);
        administrador.setGrupo(savedGrupo);
        usuarioRepository.save(administrador);

        log.info("✅ Grupo creado: {} - Tipo: {}", savedGrupo.getNombre(), savedGrupo.getTipo());
    }

    /**
     * Crear perfil personal para un usuario administrador
     */
    private void crearPerfilPersonal(Usuarios administrador) {
        Perfil perfilPersonal = Perfil.builder()
                .nombre(administrador.getNombre())
                .email(administrador.getEmail())
                .telefono(administrador.getTelefono())
                .direccion(administrador.getDireccion())
                .tipo(TipoPerfil.PERSONAL)
                .usuarioAdministrador(administrador)
                .grupo(administrador.getGrupo())
                .activo(true)
                .tieneAccesoIndependiente(false)
                .fechaCreacion(LocalDateTime.now())
                .build();

        Perfil savedPerfil = perfilRepository.save(perfilPersonal);

        // Agregar el perfil a la lista del administrador
        administrador.getPerfilesAdministrados().add(savedPerfil);
        usuarioRepository.save(administrador);

        log.info("✅ Perfil personal creado para: {}", administrador.getEmail());
    }

    /**
     * Buscar usuario por email
     */
    public Usuarios getUsuarioByEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("Usuario no encontrado: " + email));
    }

    /**
     * Buscar usuario por ID
     */
    public Usuarios getUsuarioById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new CustomException("Usuario no encontrado con ID: " + id));
    }

    /**
     * Buscar usuario con sus perfiles
     */
    public Usuarios getUsuarioWithPerfiles(Long id) {
        return usuarioRepository.findByIdWithPerfiles(id)
                .orElseThrow(() -> new CustomException("Usuario no encontrado con ID: " + id));
    }

    /**
     * Obtener todos los usuarios
     */
    public List<Usuarios> getAllUsuarios() {
        return usuarioRepository.findAll();
    }

    /**
     * Obtener usuarios por rol
     */
    public List<Usuarios> getUsuariosByRol(TipoRol rol) {
        return usuarioRepository.findByRol(rol);
    }

    /**
     * Obtener usuarios de un grupo
     */
    public List<Usuarios> getUsuariosByGrupo(Long grupoId) {
        return usuarioRepository.findByGrupoId(grupoId);
    }

    /**
     * Actualizar usuario
     */
    public Usuarios actualizarUsuario(Long id, RegistroRequest request) {
        Usuarios usuario = getUsuarioById(id);

        usuario.setNombre(request.getNombre());
        usuario.setTelefono(request.getTelefono());
        usuario.setDireccion(request.getDireccion());

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        usuario.setFechaActualizacion(LocalDateTime.now());

        Usuarios updatedUsuario = usuarioRepository.save(usuario);
        log.info("✅ Usuario actualizado: {}", updatedUsuario.getEmail());

        return updatedUsuario;
    }

    /**
     * Deshabilitar usuario
     */
    public void deshabilitarUsuario(Long id) {
        Usuarios usuario = getUsuarioById(id);
        usuario.setEnabled(false);
        usuario.setFechaActualizacion(LocalDateTime.now());
        usuarioRepository.save(usuario);

        log.info("⚠️ Usuario deshabilitado: {}", usuario.getEmail());
    }

    /**
     * Habilitar usuario
     */
    public void habilitarUsuario(Long id) {
        Usuarios usuario = getUsuarioById(id);
        usuario.setEnabled(true);
        usuario.setFechaActualizacion(LocalDateTime.now());
        usuarioRepository.save(usuario);

        log.info("✅ Usuario habilitado: {}", usuario.getEmail());
    }

    /**
     * Cambiar contraseña de usuario
     */
    public void cambiarPassword(Long id, String nuevaPassword) {
        Usuarios usuario = getUsuarioById(id);
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuario.setFechaActualizacion(LocalDateTime.now());
        usuarioRepository.save(usuario);

        log.info("🔑 Contraseña actualizada para: {}", usuario.getEmail());
    }

    /**
     * Validar que un usuario tenga acceso a un perfil
     */
    public void validarAccesoAPerfil(Long usuarioId, Long perfilId) {
        Usuarios usuario = getUsuarioById(usuarioId);
        Perfil perfil = perfilRepository.findById(perfilId)
                .orElseThrow(() -> new CustomException("Perfil no encontrado"));

        // Si es administrador, verificar que administre el perfil
        if (usuario.esAdministradorDePerfiles()) {
            if (!perfil.getUsuarioAdministrador().getId().equals(usuarioId)) {
                throw new CustomException("No tienes permiso para acceder a este perfil");
            }
            return;
        }

        // Si es perfil independiente, verificar que sea su perfil asociado
        if (usuario.esPerfilIndependiente()) {
            if (usuario.getPerfilAsociado() == null ||
                    !usuario.getPerfilAsociado().getId().equals(perfilId)) {
                throw new CustomException("No tienes permiso para acceder a este perfil");
            }
            return;
        }

        throw new CustomException("Tipo de usuario no válido");
    }
}

