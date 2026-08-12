package Bumerak.administrador.controladores;

import Bumerak.administrador.entidades.Movimiento;
import Bumerak.administrador.servicios.MovimientoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import Bumerak.administrador.dto.request.MovimientoRequest;
import Bumerak.administrador.dto.response.MovimientoResponse;
import Bumerak.administrador.dto.response.ResumenMensualResponse;
import Bumerak.administrador.entidades.Perfil;
import Bumerak.administrador.entidades.Usuarios;
import Bumerak.administrador.exception.CustomException;
import Bumerak.administrador.seguridad.jwt.JwtUtil;
import Bumerak.administrador.servicios.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MovimientosController {

    private final MovimientoService movimientoService;
    private final UsuarioService usuarioService;

    /**
     * Crear un movimiento
     * POST /api/movimientos
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MovimientoResponse> crearMovimiento(@Valid @RequestBody MovimientoRequest request) {
        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("📝 Creando movimiento para: {}", usuario.getEmail());

        Movimiento movimiento = movimientoService.crearMovimiento(request, usuario.getId());

        log.info("✅ Movimiento creado: {}", movimiento.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(MovimientoResponse.fromEntity(movimiento));
    }

    /**
     * Obtener todos los movimientos del usuario autenticado
     * GET /api/movimientos
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<MovimientoResponse>> getMisMovimientos(
            @PageableDefault(size = 20, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable) {

        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("📋 Obteniendo movimientos para: {}", usuario.getEmail());

        Page<Movimiento> movimientos;

        // Si es administrador, ve todos los movimientos de todos sus perfiles
        if (usuario.esAdministradorDePerfiles()) {
            movimientos = movimientoService.getMovimientosByAdministrador(usuario.getId(), pageable);
        } else {
            // Si es perfil independiente, ve solo sus movimientos
            movimientos = movimientoService.getMovimientosByUsuario(usuario.getId(), pageable);
        }

        return ResponseEntity.ok(movimientos.map(MovimientoResponse::fromEntity));
    }

    /**
     * Obtener movimientos de un perfil específico
     * GET /api/movimientos/perfil/{perfilId}
     */
    @GetMapping("/perfil/{perfilId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<MovimientoResponse>> getMovimientosByPerfil(
            @PathVariable Long perfilId,
            @PageableDefault(size = 20, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable) {

        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("📋 Obteniendo movimientos del perfil: {} para: {}", perfilId, usuario.getEmail());

        // Validar acceso al perfil
        usuarioService.validarAccesoAPerfil(usuario.getId(), perfilId);

        Page<Movimiento> movimientos = movimientoService.getMovimientosByPerfil(perfilId, pageable);

        return ResponseEntity.ok(movimientos.map(MovimientoResponse::fromEntity));
    }

    /**
     * Obtener un movimiento por ID
     * GET /api/movimientos/{movimientoId}
     */
    @GetMapping("/{movimientoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MovimientoResponse> getMovimiento(@PathVariable Long movimientoId) {
        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("📋 Obteniendo movimiento ID: {} para: {}", movimientoId, usuario.getEmail());

        Movimiento movimiento = movimientoService.getMovimientoById(movimientoId);

        // Validar que el usuario tenga acceso al movimiento
        validarAccesoMovimiento(usuario, movimiento);

        return ResponseEntity.ok(MovimientoResponse.fromEntity(movimiento));
    }

    /**
     * Actualizar un movimiento
     * PUT /api/movimientos/{movimientoId}
     */
    @PutMapping("/{movimientoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MovimientoResponse> actualizarMovimiento(
            @PathVariable Long movimientoId,
            @Valid @RequestBody MovimientoRequest request) {

        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("✏️ Actualizando movimiento ID: {} para: {}", movimientoId, usuario.getEmail());

        Movimiento movimiento = movimientoService.actualizarMovimiento(movimientoId, request, usuario.getId());

        log.info("✅ Movimiento actualizado: {}", movimientoId);
        return ResponseEntity.ok(MovimientoResponse.fromEntity(movimiento));
    }

    /**
     * Eliminar un movimiento (desactivar)
     * DELETE /api/movimientos/{movimientoId}
     */
    @DeleteMapping("/{movimientoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> eliminarMovimiento(@PathVariable Long movimientoId) {
        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("🗑️ Eliminando movimiento ID: {} para: {}", movimientoId, usuario.getEmail());

        movimientoService.eliminarMovimiento(movimientoId, usuario.getId());

        log.info("✅ Movimiento eliminado: {}", movimientoId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Resumen mensual de un perfil
     * GET /api/movimientos/perfil/{perfilId}/resumen/mensual?year=2026&month=7
     */
    @GetMapping("/perfil/{perfilId}/resumen/mensual")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResumenMensualResponse> getResumenMensualPerfil(
            @PathVariable Long perfilId,
            @RequestParam int year,
            @RequestParam int month) {

        Usuarios usuario = JwtUtil.getCurrentUser()
                .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

        log.info("📊 Obteniendo resumen mensual del perfil: {} - {}/{} para: {}", perfilId, month, year, usuario.getEmail());

        // Validar acceso al perfil
        usuarioService.validarAccesoAPerfil(usuario.getId(), perfilId);

        YearMonth mes = YearMonth.of(year, month);
        ResumenMensualResponse resumen = movimientoService.getResumenMensualPerfil(perfilId, mes);

        return ResponseEntity.ok(resumen);
    }

    // ========== MÉTODOS PRIVADOS ==========

    private void validarAccesoMovimiento(Usuarios usuario, Movimiento movimiento) {
        // Si es administrador, puede ver cualquier movimiento de sus perfiles
        if (usuario.esAdministradorDePerfiles()) {
            Perfil perfil = movimiento.getPerfil();
            if (perfil != null && !perfil.getUsuarioAdministrador().getId().equals(usuario.getId())) {
                throw new CustomException("No tienes permiso para ver este movimiento", HttpStatus.FORBIDDEN);
            }
            return;
        }

        // Si es perfil independiente, solo puede ver sus movimientos
        if (usuario.esPerfilIndependiente()) {
            if (usuario.getPerfilAsociado() == null ||
                    !usuario.getPerfilAsociado().getId().equals(movimiento.getPerfil().getId())) {
                throw new CustomException("No tienes permiso para ver este movimiento", HttpStatus.FORBIDDEN);
            }
            return;
        }

        throw new CustomException("No tienes permiso para ver este movimiento", HttpStatus.FORBIDDEN);
    }
}
