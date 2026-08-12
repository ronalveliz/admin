package Bumerak.administrador.servicios;

import Bumerak.administrador.dto.request.PerfilRequest;
import Bumerak.administrador.entidades.Perfil;
import Bumerak.administrador.entidades.Usuarios;
import Bumerak.administrador.entidades.enums.TipoPerfil;
import Bumerak.administrador.entidades.enums.TipoRol;
import Bumerak.administrador.exception.CustomException;
import Bumerak.administrador.repositorios.PerfilRepository;
import Bumerak.administrador.repositorios.UsuariosRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PerfilService {

    private final PerfilRepository perfilRepository;
    private final UsuariosRepository usuariosRepository;
    private final PasswordEncoder passwordEncoder;
    private final GrupoService grupoService;

    /**
     * Crear un nuevo perfil para un administrador
     */
    public Perfil crearPerfil(PerfilRequest request, Long administradorId, boolean crearUsuarioIndependiente) {
        log.info("📝 Creando perfil para administrador ID: {}", administradorId);

        Usuarios administrador = usuariosRepository.findById(administradorId)
                .orElseThrow(() -> new CustomException("Administrador no encontrado", HttpStatus.NOT_FOUND));

        // Validar que el administrador pueda crear perfiles
        if (!administrador.esAdministradorDePerfiles()) {
            throw new CustomException("El usuario no tiene permisos para crear perfiles", HttpStatus.FORBIDDEN);
        }

        // Validar que tenga un grupo
        if (administrador.getGrupo() == null) {
            throw new CustomException("El administrador debe tener un grupo para crear perfiles", HttpStatus.BAD_REQUEST);
        }

        // Validar tipo de perfil compatible con el grupo
        grupoService.validarPerfilParaGrupo(
                administrador.getGrupo().getTipo(),
                request.getTipo()
        );

        // Validar que no exista un perfil con el mismo email
        if (request.getEmail() != null && perfilRepository.existsByEmail(request.getEmail())) {
            throw new CustomException("Ya existe un perfil con el email: " + request.getEmail(), HttpStatus.CONFLICT);
        }

        // Crear el perfil - usando los getters de Lombok
        Perfil perfil = Perfil.builder()
                .nombre(request.getNombre())
                .apellidos(request.getApellidos())
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .direccion(request.getDireccion())
                .fotoPerfil(request.getFotoPerfil())
                .fechaNacimiento(request.getFechaNacimiento())
                .tipo(request.getTipo())
                .usuarioAdministrador(administrador)
                .grupo(administrador.getGrupo())
                .activo(true)
                .tieneAccesoIndependiente(crearUsuarioIndependiente)
                .fechaCreacion(LocalDateTime.now())
                .build();

        Perfil savedPerfil = perfilRepository.save(perfil);

        // Si el perfil debe tener acceso independiente, crear usuario asociado
        if (crearUsuarioIndependiente) {
            Usuarios usuarioAsociado = crearUsuarioParaPerfil(savedPerfil, administrador);
            savedPerfil.setUsuarioAsociado(usuarioAsociado);
            savedPerfil = perfilRepository.save(savedPerfil);
        }

        // Agregar el perfil a la lista del administrador
        administrador.getPerfilesAdministrados().add(savedPerfil);
        usuariosRepository.save(administrador);

        log.info("✅ Perfil creado: {} - Acceso independiente: {}",
                savedPerfil.getNombreCompleto(),
                crearUsuarioIndependiente ? "SÍ" : "NO");

        return savedPerfil;
    }

    /**
     * Crear usuario independiente para un perfil
     */
    private Usuarios crearUsuarioParaPerfil(Perfil perfil, Usuarios administrador) {
        // Validar que el email esté disponible
        if (usuariosRepository.existsByEmail(perfil.getEmail())) {
            throw new CustomException("El email " + perfil.getEmail() + " ya está en uso", HttpStatus.CONFLICT);
        }

        // Determinar el rol según el tipo de perfil
        TipoRol rol;
        if (perfil.getTipo() == TipoPerfil.MIEMBRO) {
            rol = TipoRol.MIEMBRO;
        } else if (perfil.getTipo() == TipoPerfil.EMPLEADO) {
            rol = TipoRol.EMPLEADO;
        } else {
            throw new CustomException("Tipo de perfil no válido para acceso independiente", HttpStatus.BAD_REQUEST);
        }

        // Generar contraseña temporal
        String passwordTemporal = generarPasswordTemporal();

        // Crear el usuario
        Usuarios usuarioAsociado = Usuarios.builder()
                .email(perfil.getEmail())
                .password(passwordEncoder.encode(passwordTemporal))
                .nombre(perfil.getNombre())
                .rol(rol)
                .telefono(perfil.getTelefono())
                .direccion(perfil.getDireccion())
                .fotoPerfil(perfil.getFotoPerfil())
                .grupo(administrador.getGrupo())
                .perfilAsociado(perfil)
                .enabled(true)
                .fechaCreacion(LocalDateTime.now())
                .build();

        Usuarios savedUsuario = usuariosRepository.save(usuarioAsociado);

        log.info("🔑 Usuario creado para perfil: {} - Email: {} - Password temporal: {}",
                perfil.getNombreCompleto(),
                savedUsuario.getEmail(),
                passwordTemporal);

        return savedUsuario;
    }

    /**
     * Generar contraseña temporal
     */
    private String generarPasswordTemporal() {
        return UUID.randomUUID().toString().substring(0, 8) + "Temp2026!";
    }

    /**
     * Obtener perfil por ID
     */
    public Perfil getPerfilById(Long id) {
        return perfilRepository.findById(id)
                .orElseThrow(() -> new CustomException("Perfil no encontrado con ID: " + id, HttpStatus.NOT_FOUND));
    }

    /**
     * Obtener perfil con movimientos
     */
    public Perfil getPerfilWithMovimientos(Long id) {
        return perfilRepository.findByIdWithMovimientos(id)
                .orElseThrow(() -> new CustomException("Perfil no encontrado con ID: " + id, HttpStatus.NOT_FOUND));
    }

    /**
     * Obtener perfiles de un administrador
     */
    public List<Perfil> getPerfilesByAdministrador(Long administradorId) {
        Usuarios administrador = usuariosRepository.findById(administradorId)
                .orElseThrow(() -> new CustomException("Administrador no encontrado", HttpStatus.NOT_FOUND));

        return administrador.getPerfilesAdministrados();
    }

    /**
     * Obtener perfiles activos de un administrador
     */
    public List<Perfil> getPerfilesActivosByAdministrador(Long administradorId) {
        return perfilRepository.findByUsuarioAdministradorIdAndActivoTrue(administradorId);
    }

    /**
     * Obtener perfiles de un grupo
     */
    public List<Perfil> getPerfilesByGrupo(Long grupoId) {
        return perfilRepository.findByGrupoIdAndActivoTrue(grupoId);
    }

    /**
     * Obtener perfiles por tipo
     */
    public List<Perfil> getPerfilesByTipo(TipoPerfil tipo) {
        return perfilRepository.findByTipo(tipo);
    }

    /**
     * Actualizar perfil
     */
    public Perfil actualizarPerfil(Long id, PerfilRequest request, Long administradorId) {
        Perfil perfil = getPerfilById(id);

        // Validar que el administrador tenga permiso
        if (!perfil.getUsuarioAdministrador().getId().equals(administradorId)) {
            throw new CustomException("No tienes permiso para modificar este perfil", HttpStatus.FORBIDDEN);
        }

        // Validar que el email no esté en uso por otro perfil
        if (request.getEmail() != null && !request.getEmail().equals(perfil.getEmail())) {
            if (perfilRepository.existsByEmail(request.getEmail())) {
                throw new CustomException("El email " + request.getEmail() + " ya está en uso", HttpStatus.CONFLICT);
            }
        }

        // Actualizar datos
        perfil.setNombre(request.getNombre());
        perfil.setApellidos(request.getApellidos());
        perfil.setEmail(request.getEmail());
        perfil.setTelefono(request.getTelefono());
        perfil.setDireccion(request.getDireccion());
        perfil.setFotoPerfil(request.getFotoPerfil());
        perfil.setFechaNacimiento(request.getFechaNacimiento());
        perfil.setFechaActualizacion(LocalDateTime.now());

        Perfil updatedPerfil = perfilRepository.save(perfil);
        log.info("✅ Perfil actualizado: {}", updatedPerfil.getNombreCompleto());

        return updatedPerfil;
    }

    /**
     * Deshabilitar perfil (eliminación lógica)
     */
    public void deshabilitarPerfil(Long id, Long administradorId) {
        Perfil perfil = getPerfilById(id);

        // Validar que el administrador tenga permiso
        if (!perfil.getUsuarioAdministrador().getId().equals(administradorId)) {
            throw new CustomException("No tienes permiso para deshabilitar este perfil", HttpStatus.FORBIDDEN);
        }

        // No permitir deshabilitar el perfil personal
        if (perfil.esPersonal()) {
            throw new CustomException("No se puede deshabilitar el perfil personal", HttpStatus.BAD_REQUEST);
        }

        perfil.setActivo(false);
        perfil.setFechaActualizacion(LocalDateTime.now());
        perfilRepository.save(perfil);

        // Si tiene usuario asociado, deshabilitarlo también
        if (perfil.getUsuarioAsociado() != null) {
            Usuarios usuario = perfil.getUsuarioAsociado();
            usuario.setEnabled(false);
            usuario.setFechaActualizacion(LocalDateTime.now());
            usuariosRepository.save(usuario);
            log.info("⚠️ Usuario asociado deshabilitado: {}", usuario.getEmail());
        }

        log.info("⚠️ Perfil deshabilitado: {}", perfil.getNombreCompleto());
    }

    /**
     * Habilitar perfil
     */
    public void habilitarPerfil(Long id, Long administradorId) {
        Perfil perfil = getPerfilById(id);

        // Validar que el administrador tenga permiso
        if (!perfil.getUsuarioAdministrador().getId().equals(administradorId)) {
            throw new CustomException("No tienes permiso para habilitar este perfil", HttpStatus.FORBIDDEN);
        }

        perfil.setActivo(true);
        perfil.setFechaActualizacion(LocalDateTime.now());
        perfilRepository.save(perfil);

        // Si tiene usuario asociado, habilitarlo también
        if (perfil.getUsuarioAsociado() != null) {
            Usuarios usuario = perfil.getUsuarioAsociado();
            usuario.setEnabled(true);
            usuario.setFechaActualizacion(LocalDateTime.now());
            usuariosRepository.save(usuario);
            log.info("✅ Usuario asociado habilitado: {}", usuario.getEmail());
        }

        log.info("✅ Perfil habilitado: {}", perfil.getNombreCompleto());
    }

    /**
     * Cambiar contraseña de un perfil independiente
     */
    public void cambiarPasswordPerfil(Long perfilId, String nuevaPassword, Long administradorId) {
        Perfil perfil = getPerfilById(perfilId);

        // Validar que el administrador tenga permiso
        if (!perfil.getUsuarioAdministrador().getId().equals(administradorId)) {
            throw new CustomException("No tienes permiso para cambiar la contraseña de este perfil", HttpStatus.FORBIDDEN);
        }

        if (!perfil.tieneAccesoIndependiente()) {
            throw new CustomException("Este perfil no tiene acceso independiente", HttpStatus.BAD_REQUEST);
        }

        Usuarios usuarioAsociado = perfil.getUsuarioAsociado();
        if (usuarioAsociado == null) {
            throw new CustomException("El perfil no tiene un usuario asociado", HttpStatus.NOT_FOUND);
        }

        usuarioAsociado.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioAsociado.setFechaActualizacion(LocalDateTime.now());
        usuariosRepository.save(usuarioAsociado);

        log.info("🔑 Contraseña actualizada para perfil: {}", perfil.getNombreCompleto());
    }

    /**
     * Cambiar contraseña de un perfil (para el propio perfil independiente)
     */
    public void cambiarMiPassword(Long perfilId, String passwordActual, String nuevaPassword) {
        Perfil perfil = getPerfilById(perfilId);

        if (!perfil.tieneAccesoIndependiente()) {
            throw new CustomException("Este perfil no tiene acceso independiente", HttpStatus.BAD_REQUEST);
        }

        Usuarios usuarioAsociado = perfil.getUsuarioAsociado();
        if (usuarioAsociado == null) {
            throw new CustomException("El perfil no tiene un usuario asociado", HttpStatus.NOT_FOUND);
        }

        // Verificar contraseña actual
        if (!passwordEncoder.matches(passwordActual, usuarioAsociado.getPassword())) {
            throw new CustomException("Contraseña actual incorrecta", HttpStatus.UNAUTHORIZED);
        }

        usuarioAsociado.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioAsociado.setFechaActualizacion(LocalDateTime.now());
        usuariosRepository.save(usuarioAsociado);

        log.info("🔑 Contraseña actualizada por el propio perfil: {}", perfil.getNombreCompleto());
    }

    /**
     * Validar que un perfil pertenezca a un usuario
     */
    public void validarPerfilPerteneceAUsuario(Long perfilId, Long usuarioId) {
        Perfil perfil = getPerfilById(perfilId);
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new CustomException("Usuario no encontrado", HttpStatus.NOT_FOUND));

        // Si es administrador, verificar que administre el perfil
        if (usuario.esAdministradorDePerfiles()) {
            if (!perfil.getUsuarioAdministrador().getId().equals(usuarioId)) {
                throw new CustomException("No tienes permiso para acceder a este perfil", HttpStatus.FORBIDDEN);
            }
            return;
        }

        // Si es perfil independiente, verificar que sea su perfil asociado
        if (usuario.esPerfilIndependiente()) {
            if (usuario.getPerfilAsociado() == null ||
                    !usuario.getPerfilAsociado().getId().equals(perfilId)) {
                throw new CustomException("No tienes permiso para acceder a este perfil", HttpStatus.FORBIDDEN);
            }
            return;
        }

        throw new CustomException("Tipo de usuario no válido", HttpStatus.FORBIDDEN);
    }

    /**
     * Contar perfiles por administrador
     */
    public long countPerfilesByAdministrador(Long administradorId) {
        return perfilRepository.countByUsuarioAdministradorId(administradorId);
    }

    /**
     * Contar perfiles activos por administrador
     */
    public long countPerfilesActivosByAdministrador(Long administradorId) {
        return perfilRepository.countByUsuarioAdministradorIdAndActivoTrue(administradorId);
    }
}
