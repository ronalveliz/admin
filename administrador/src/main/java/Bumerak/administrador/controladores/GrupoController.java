package Bumerak.administrador.controladores;
import Bumerak.administrador.dto.response.GrupoResponse;
import Bumerak.administrador.dto.response.ResumenMensualResponse;
import Bumerak.administrador.entidades.Grupo;
import Bumerak.administrador.entidades.Usuarios;
import Bumerak.administrador.exception.CustomException;
import Bumerak.administrador.seguridad.jwt.JwtUtil;
import Bumerak.administrador.servicios.GrupoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/grupos")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class GrupoController {

    private final GrupoService grupoService;

    /**
     * Obtener el grupo del usuario autenticado
     * GET /api/grupos/mi-grupo
     */
    @GetMapping("/mi-grupo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GrupoResponse> getMiGrupo() {
        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("📋 Obteniendo grupo para: {}", usuario.getEmail());

        if (usuario.getGrupo() == null) {
            throw new CustomException("El usuario no pertenece a ningún grupo", HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(GrupoResponse.fromEntity(usuario.getGrupo()));
    }

    /**
     * Obtener un grupo por ID
     * GET /api/grupos/{grupoId}
     */
    @GetMapping("/{grupoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GrupoResponse> getGrupo(@PathVariable Long grupoId) {
        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("📋 Obteniendo grupo ID: {} para: {}", grupoId, usuario.getEmail());

        Grupo grupo = grupoService.getGrupoById(grupoId);

        // Validar acceso
        grupoService.validarAccesoAGrupo(grupo, usuario);

        return ResponseEntity.ok(GrupoResponse.fromEntity(grupo));
    }

    /**
     * Actualizar grupo
     * PUT /api/grupos/{grupoId}
     */
    @PutMapping("/{grupoId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR', 'ROLE_FAMILIA', 'ROLE_EMPRESA')")
    public ResponseEntity<GrupoResponse> actualizarGrupo(
            @PathVariable Long grupoId,
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion) {

        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("✏️ Actualizando grupo ID: {} para: {}", grupoId, usuario.getEmail());

        Grupo grupo = grupoService.getGrupoById(grupoId);

        // Solo el administrador del grupo puede actualizarlo
        if (!grupo.esAdministrador(usuario)) {
            throw new CustomException("No tienes permiso para actualizar este grupo", HttpStatus.FORBIDDEN);
        }

        Grupo updatedGrupo = grupoService.actualizarGrupo(grupoId, nombre, descripcion);

        log.info("✅ Grupo actualizado: {}", updatedGrupo.getNombre());
        return ResponseEntity.ok(GrupoResponse.fromEntity(updatedGrupo));
    }

    /**
     * Deshabilitar grupo
     * DELETE /api/grupos/{grupoId}
     */
    @DeleteMapping("/{grupoId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR', 'ROLE_FAMILIA', 'ROLE_EMPRESA')")
    public ResponseEntity<Void> deshabilitarGrupo(@PathVariable Long grupoId) {
        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("🗑️ Deshabilitando grupo ID: {} para: {}", grupoId, usuario.getEmail());

        Grupo grupo = grupoService.getGrupoById(grupoId);

        // Solo el administrador del grupo puede deshabilitarlo
        if (!grupo.esAdministrador(usuario)) {
            throw new CustomException("No tienes permiso para deshabilitar este grupo", HttpStatus.FORBIDDEN);
        }

        grupoService.deshabilitarGrupo(grupoId);

        log.info("✅ Grupo deshabilitado: {}", grupoId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Resumen mensual del grupo
     * GET /api/grupos/{grupoId}/resumen/mensual?year=2026&month=7
     */
    @GetMapping("/{grupoId}/resumen/mensual")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResumenMensualResponse> getResumenMensual(
            @PathVariable Long grupoId,
            @RequestParam int year,
            @RequestParam int month) {

        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("📊 Obteniendo resumen mensual para grupo: {} - {}/{}", grupoId, month, year);

        YearMonth mes = YearMonth.of(year, month);
        ResumenMensualResponse resumen = grupoService.getResumenMensualGrupo(grupoId, mes, usuario.getId());

        return ResponseEntity.ok(resumen);
    }
}