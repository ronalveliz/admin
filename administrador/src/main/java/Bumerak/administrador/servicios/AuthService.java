package Bumerak.administrador.servicios;

import Bumerak.administrador.dto.LoginUsuarios;
import Bumerak.administrador.dto.NuevoUsuariosRegistrado;
import Bumerak.administrador.dto.response.AuthResponse;
import Bumerak.administrador.dto.response.PerfilResponse;
import Bumerak.administrador.entidades.Grupo;
import Bumerak.administrador.entidades.Perfil;
import Bumerak.administrador.entidades.Usuarios;
import Bumerak.administrador.entidades.enums.TipoPerfil;
import Bumerak.administrador.exception.CustomException;
import Bumerak.administrador.repositorios.GrupoRepository;
import Bumerak.administrador.repositorios.PerfilRepository;
import Bumerak.administrador.repositorios.UsuariosRepository;
import Bumerak.administrador.seguridad.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UsuariosRepository usuariosRepository;
    private final PerfilRepository perfilRepository;
    private final GrupoRepository grupoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Registrar nuevo usuario
     */
    public AuthResponse registrar(NuevoUsuariosRegistrado request) {
        log.info("📝 Registrando nuevo usuario: {}", request.email());

        try {
            // 1. Validar que el email no exista
            if (usuariosRepository.existsByEmail(request.email())) {
                throw new CustomException("El email " + request.email() + " ya está registrado", HttpStatus.CONFLICT);
            }

            // 2. Validar que el rol sea válido para administrador
            if (!request.roleName().esAdministrador()) {
                throw new CustomException("El rol " + request.roleName() + " no es válido para registro", HttpStatus.BAD_REQUEST);
            }

            // 3. Crear el usuario
            Usuarios usuario = Usuarios.builder()
                    .email(request.email())
                    .password(passwordEncoder.encode(request.password()))
                    .nombre(request.nombre())
                    .rol(request.roleName())
                    .telefono(request.telefono())
                    .fotoPerfil(request.imgUser())
                    .enabled(true)
                    .fechaCreacion(LocalDateTime.now())
                    .build();

            Usuarios savedUsuario = usuariosRepository.save(usuario);
            log.info("✅ Usuario creado: {}", savedUsuario.getEmail());

            // 4. Crear grupo si se especifica
            Grupo grupo = null;
            if (request.nombreGrupo() != null && !request.nombreGrupo().isEmpty()) {
                grupo = crearGrupo(savedUsuario, request.nombreGrupo(), request.descripcionGrupo());
            }

            // 5. Crear perfil personal
            Perfil perfilPersonal = crearPerfilPersonal(savedUsuario, grupo);

            // 6. Generar token
            String token = jwtUtil.generarTokenConPerfil(savedUsuario, perfilPersonal);

            return construirAuthResponse(savedUsuario, perfilPersonal, token);

        } catch (CustomException e) {
            log.error("❌ Error en registro: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Error inesperado en registro: {}", e.getMessage(), e);
            throw new CustomException("Error al registrar usuario: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Login de usuario
     */
    public AuthResponse autenticar(LoginUsuarios request) {
        log.info("🔐 Intentando autenticar usuario: {}", request.email());

        try {
            // 1. Validar credenciales
            Usuarios usuario = usuariosRepository.findByEmail(request.email())
                    .orElseThrow(() -> new CustomException("Credenciales inválidas", HttpStatus.UNAUTHORIZED));

            if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
                throw new CustomException("Credenciales inválidas", HttpStatus.UNAUTHORIZED);
            }

            if (!usuario.getEnabled()) {
                throw new CustomException("Usuario deshabilitado", HttpStatus.FORBIDDEN);
            }

            log.info("✅ Usuario autenticado: {}", usuario.getEmail());

            // 2. Determinar perfil activo
            Perfil perfilActivo = usuario.getPerfilPersonal();
            if (perfilActivo == null) {
                throw new CustomException("No se encontró el perfil personal", HttpStatus.NOT_FOUND);
            }

            // 3. Generar token
            String token = jwtUtil.generarTokenConPerfil(usuario, perfilActivo);

            return construirAuthResponse(usuario, perfilActivo, token);

        } catch (CustomException e) {
            log.error("❌ Error en login: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Error inesperado en login: {}", e.getMessage(), e);
            throw new CustomException("Error al autenticar usuario: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Login con perfil específico
     */
    public AuthResponse autenticarConPerfil(LoginUsuarios request, Long perfilId) {
        log.info("🔐 Intentando autenticar usuario: {} con perfil ID: {}", request.email(), perfilId);

        try {
            Usuarios usuario = usuariosRepository.findByEmail(request.email())
                    .orElseThrow(() -> new CustomException("Credenciales inválidas", HttpStatus.UNAUTHORIZED));

            if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
                throw new CustomException("Credenciales inválidas", HttpStatus.UNAUTHORIZED);
            }

            if (!usuario.getEnabled()) {
                throw new CustomException("Usuario deshabilitado", HttpStatus.FORBIDDEN);
            }

            // Buscar el perfil
            Perfil perfilActivo = perfilRepository.findById(perfilId)
                    .orElseThrow(() -> new CustomException("Perfil no encontrado", HttpStatus.NOT_FOUND));

            // Validar que el perfil pertenezca al usuario
            if (!perfilActivo.getUsuarioAdministrador().getId().equals(usuario.getId())) {
                throw new CustomException("No tienes permiso para acceder a este perfil", HttpStatus.FORBIDDEN);
            }

            if (!perfilActivo.getActivo()) {
                throw new CustomException("El perfil está desactivado", HttpStatus.FORBIDDEN);
            }

            String token = jwtUtil.generarTokenConPerfil(usuario, perfilActivo);
            return construirAuthResponse(usuario, perfilActivo, token);

        } catch (CustomException e) {
            log.error("❌ Error en login con perfil: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Error inesperado en login con perfil: {}", e.getMessage(), e);
            throw new CustomException("Error al autenticar con perfil: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ========== MÉTODOS PRIVADOS ==========

    private Grupo crearGrupo(Usuarios administrador, String nombre, String descripcion) {
        Grupo grupo = Grupo.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .tipo(administrador.getRol().getTipoGrupoAsociado())
                .administrador(administrador)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();

        Grupo savedGrupo = grupoRepository.save(grupo);
        administrador.setGrupo(savedGrupo);
        usuariosRepository.save(administrador);

        log.info("✅ Grupo creado: {} - Tipo: {}", savedGrupo.getNombre(), savedGrupo.getTipo());
        return savedGrupo;
    }

    private Perfil crearPerfilPersonal(Usuarios administrador, Grupo grupo) {
        Perfil perfilPersonal = Perfil.builder()
                .nombre(administrador.getNombre())
                .email(administrador.getEmail())
                .telefono(administrador.getTelefono())
                .fotoPerfil(administrador.getFotoPerfil())
                .tipo(TipoPerfil.PERSONAL)
                .usuarioAdministrador(administrador)
                .grupo(grupo)
                .activo(true)
                .tieneAccesoIndependiente(false)
                .fechaCreacion(LocalDateTime.now())
                .build();

        Perfil savedPerfil = perfilRepository.save(perfilPersonal);
        administrador.getPerfilesAdministrados().add(savedPerfil);
        usuariosRepository.save(administrador);

        log.info("✅ Perfil personal creado para: {}", administrador.getEmail());
        return savedPerfil;
    }

    private AuthResponse construirAuthResponse(Usuarios usuario, Perfil perfilActivo, String token) {
        List<Perfil> perfilesAccesibles = usuario.getPerfilesAccesibles();

        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .expiracion(jwtUtil.getExpirationTime())
                .usuario(construirUsuarioInfo(usuario))
                .perfilActivo(PerfilResponse.fromEntity(perfilActivo))
                .todosLosPerfiles(perfilesAccesibles.stream()
                        .map(PerfilResponse::fromEntity)
                        .collect(Collectors.toList()))
                .grupo(construirGrupoInfo(usuario))
                .permisos(obtenerPermisos(usuario))
                .build();
    }

    private AuthResponse.UsuarioInfo construirUsuarioInfo(Usuarios usuario) {
        return AuthResponse.UsuarioInfo.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .rol(usuario.getRol().name())
                .telefono(usuario.getTelefono())
                .direccion(usuario.getDireccion())
                .fotoPerfil(usuario.getFotoPerfil())
                .build();
    }

    private AuthResponse.GrupoInfo construirGrupoInfo(Usuarios usuario) {
        if (usuario.getGrupo() == null) {
            return null;
        }

        return AuthResponse.GrupoInfo.builder()
                .id(usuario.getGrupo().getId())
                .nombre(usuario.getGrupo().getNombre())
                .tipo(usuario.getGrupo().getTipo().name())
                .descripcion(usuario.getGrupo().getDescripcion())
                .build();
    }

    private List<String> obtenerPermisos(Usuarios usuario) {
        List<String> permisos = new ArrayList<>();
        permisos.add("VER_MIS_MOVIMIENTOS");
        permisos.add("CREAR_MOVIMIENTOS");

        if (usuario.esAdministradorDePerfiles()) {
            permisos.add("VER_TODOS_LOS_MOVIMIENTOS");
            permisos.add("GESTIONAR_PERFILES");
            permisos.add("VER_TODOS_LOS_PERFILES");
            if (usuario.esAdminDeGrupo()) {
                permisos.add("GESTIONAR_GRUPO");
            }
        }

        if (usuario.esPerfilIndependiente()) {
            permisos.add("VER_MIS_DATOS");
            permisos.add("CAMBIAR_MI_CONTRASENA");
        }

        if (usuario.esAdministrador()) {
            permisos.add("GESTIONAR_TODO_EL_SISTEMA");
        }

        return permisos;
    }
}