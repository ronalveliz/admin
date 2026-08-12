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
import org.springframework.web.bind.annotation.*;

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
     * POST /api/perfiles
     * Solo administradores pueden crear perfiles
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR', 'ROLE_FAMILIA', 'ROLE_EMPRESA')")
    public ResponseEntity<PerfilResponse> crearPerfil(@Valid @RequestBody PerfilRequest request) {
        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("📝 Creando perfil para administrador: {}", usuario.getEmail());

        // Validar que el usuario sea administrador de perfiles
        if (!usuario.esAdministradorDePerfiles()) {
            throw new CustomException("Solo los administradores pueden crear perfiles", HttpStatus.FORBIDDEN);
        }

        // Determinar si el perfil tendrá acceso independiente
        boolean accesoIndependiente = request.getTipo() == TipoPerfil.MIEMBRO ||
                request.getTipo() == TipoPerfil.EMPLEADO;

        Perfil perfil = perfilService.crearPerfil(request, usuario.getId(), accesoIndependiente);

        log.info("✅ Perfil creado: {} - Acceso independiente: {}",
                perfil.getNombreCompleto(),
                accesoIndependiente ? "SÍ" : "NO");

        return ResponseEntity.status(HttpStatus.CREATED).body(PerfilResponse.fromEntity(perfil));
    }

    /**
     * Obtener todos los perfiles del usuario autenticado
     * GET /api/perfiles
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PerfilResponse>> getMisPerfiles() {
        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("📋 Obteniendo perfiles para: {}", usuario.getEmail());

        List<Perfil> perfiles = usuario.getPerfilesAccesibles();

        return ResponseEntity.ok(PerfilResponse.fromEntityList(perfiles));
    }

    /**
     * Obtener un perfil específico por ID
     * GET /api/perfiles/{perfilId}
     */
    @GetMapping("/{perfilId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PerfilResponse> getPerfil(@PathVariable Long perfilId) {
        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("📋 Obteniendo perfil ID: {} para usuario: {}", perfilId, usuario.getEmail());

        // Validar acceso al perfil
        usuarioService.validarAccesoAPerfil(usuario.getId(), perfilId);

        Perfil perfil = perfilService.getPerfilById(perfilId);

        return ResponseEntity.ok(PerfilResponse.fromEntity(perfil));
    }

    /**
     * Actualizar un perfil
     * PUT /api/perfiles/{perfilId}
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
     * Deshabilitar un perfil (eliminación lógica)
     * DELETE /api/perfiles/{perfilId}
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
     * PUT /api/perfiles/{perfilId}/habilitar
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
     * Cambiar contraseña de un perfil independiente (solo admin)
     * PUT /api/perfiles/{perfilId}/password
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