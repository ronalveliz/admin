package Bumerak.administrador.controladores;

import Bumerak.administrador.dto.request.PerfilRequest;
import Bumerak.administrador.dto.response.PerfilResponse;
import Bumerak.administrador.entidades.Perfil;
import Bumerak.administrador.entidades.Usuarios;
import Bumerak.administrador.entidades.enums.TipoPerfil;
import Bumerak.administrador.exception.CustomException;
import Bumerak.administrador.seguridad.jwt.JwtUtil;
import Bumerak.administrador.servicios.PerfilService;
import Bumerak.administrador.servicios.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/perfiles")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PerfilController {

    private final PerfilService perfilService;
    private final UsuarioService usuarioService;

    /**
     * Crear un nuevo perfil
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR', 'ROLE_FAMILIA', 'ROLE_EMPRESA')")
    public ResponseEntity<PerfilResponse> crearPerfil(@Valid @RequestBody PerfilRequest request) {
        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("📝 Creando perfil para administrador: {}", usuario.getEmail());

        if (!usuario.esAdministradorDePerfiles()) {
            throw new CustomException("Solo los administradores pueden crear perfiles", HttpStatus.FORBIDDEN);
        }

        boolean accesoIndependiente = request.getTipo() == TipoPerfil.MIEMBRO ||
                request.getTipo() == TipoPerfil.EMPLEADO;

        Perfil perfil = perfilService.crearPerfil(request, usuario.getId(), accesoIndependiente);
        log.info("📥 Tipo recibido del frontend: {}", request.getTipo());
        log.info("✅ Perfil creado: {} - Acceso independiente: {}",
                perfil.getNombreCompleto(),
                accesoIndependiente ? "SÍ" : "NO");

        return ResponseEntity.status(HttpStatus.CREATED).body(PerfilResponse.fromEntity(perfil));
    }

    /**
     * Obtener todos los perfiles del usuario autenticado
     * ✅ CORREGIDO: Sin LazyInitializationException
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PerfilResponse>> getMisPerfiles() {
        Usuarios usuarioAuth = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("📋 Obteniendo perfiles para: {}", usuarioAuth.getEmail());

        // ✅ Recargar el usuario desde la BD para tener sesión activa
        Usuarios usuario = usuarioService.getUsuarioById(usuarioAuth.getId());

        List<Perfil> perfiles = new ArrayList<>();

        if (usuario.esAdministradorDePerfiles()) {
            // ✅ Usar el repositorio directamente (sin lazy)
            perfiles = perfilService.getPerfilesActivosByAdministrador(usuario.getId());
        } else if (usuario.esPerfilIndependiente()) {
            // ✅ Usar el repositorio directamente
            Perfil perfilAsociado = perfilService.getPerfilByUsuarioAsociado(usuario.getId());
            if (perfilAsociado != null) {
                perfiles = List.of(perfilAsociado);
            }
        }

        log.info("✅ {} perfiles encontrados", perfiles.size());
        return ResponseEntity.ok(PerfilResponse.fromEntityList(perfiles));
    }

    /**
     * Obtener un perfil específico por ID
     */
    @GetMapping("/{perfilId}")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<PerfilResponse> getPerfil(@PathVariable Long perfilId) {
        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("📋 Obteniendo perfil ID: {} para usuario: {}", perfilId, usuario.getEmail());

        usuarioService.validarAccesoAPerfil(usuario.getId(), perfilId);

        Perfil perfil = perfilService.getPerfilById(perfilId);

        return ResponseEntity.ok(PerfilResponse.fromEntity(perfil));
    }

    /**
     * Actualizar un perfil
     */
    @PutMapping("/{perfilId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR', 'ROLE_FAMILIA', 'ROLE_EMPRESA')")
    public ResponseEntity<PerfilResponse> actualizarPerfil(
            @PathVariable Long perfilId,
            @Valid @RequestBody PerfilRequest request) {

        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("✏️ Actualizando perfil ID: {} para: {}", perfilId, usuario.getEmail());

        Perfil perfil = perfilService.actualizarPerfil(perfilId, request, usuario.getId());

        log.info("✅ Perfil actualizado: {}", perfil.getNombreCompleto());
        return ResponseEntity.ok(PerfilResponse.fromEntity(perfil));
    }

    /**
     * Deshabilitar un perfil
     */
    @DeleteMapping("/{perfilId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR', 'ROLE_FAMILIA', 'ROLE_EMPRESA')")
    public ResponseEntity<Void> deshabilitarPerfil(@PathVariable Long perfilId) {
        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("🗑️ Deshabilitando perfil ID: {} para: {}", perfilId, usuario.getEmail());

        perfilService.deshabilitarPerfil(perfilId, usuario.getId());

        log.info("✅ Perfil deshabilitado: {}", perfilId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Habilitar un perfil
     */
    @PutMapping("/{perfilId}/habilitar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR', 'ROLE_FAMILIA', 'ROLE_EMPRESA')")
    public ResponseEntity<Void> habilitarPerfil(@PathVariable Long perfilId) {
        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("🔄 Habilitando perfil ID: {} para: {}", perfilId, usuario.getEmail());

        perfilService.habilitarPerfil(perfilId, usuario.getId());

        log.info("✅ Perfil habilitado: {}", perfilId);
        return ResponseEntity.ok().build();
    }

    /**
     * Cambiar contraseña de un perfil independiente
     */
    @PutMapping("/{perfilId}/password")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR', 'ROLE_FAMILIA', 'ROLE_EMPRESA')")
    public ResponseEntity<Void> cambiarPasswordPerfil(
            @PathVariable Long perfilId,
            @RequestParam String nuevaPassword) {

        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("🔑 Cambiando contraseña para perfil ID: {} por: {}", perfilId, usuario.getEmail());

        perfilService.cambiarPasswordPerfil(perfilId, nuevaPassword, usuario.getId());

        log.info("✅ Contraseña actualizada para perfil: {}", perfilId);
        return ResponseEntity.ok().build();
    }
}