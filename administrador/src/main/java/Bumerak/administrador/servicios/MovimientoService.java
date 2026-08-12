package Bumerak.administrador.servicios;

import Bumerak.administrador.dto.request.MovimientoRequest;
import Bumerak.administrador.dto.response.ResumenMensualResponse;
import Bumerak.administrador.entidades.Movimiento;
import Bumerak.administrador.entidades.Perfil;
import Bumerak.administrador.entidades.Usuarios;
import Bumerak.administrador.entidades.enums.TipoMovimiento;
import Bumerak.administrador.exception.CustomException;
import Bumerak.administrador.repositorios.MovimientoRepository;
import Bumerak.administrador.repositorios.PerfilRepository;
import Bumerak.administrador.repositorios.UsuariosRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MovimientoService {

    private final MovimientoRepository movimientoRepository;
    private final UsuariosRepository usuariosRepository;
    private final PerfilRepository perfilRepository;

    /**
     * Crear movimiento
     */
    public Movimiento crearMovimiento(MovimientoRequest request, Long usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new CustomException("Usuario no encontrado", HttpStatus.NOT_FOUND));

        Perfil perfil = null;
        if (request.getPerfilId() != null) {
            perfil = perfilRepository.findById(request.getPerfilId())
                    .orElseThrow(() -> new CustomException("Perfil no encontrado", HttpStatus.NOT_FOUND));

            // Validar acceso al perfil
            validarAccesoAPerfil(usuario, perfil);
        } else {
            // Si no se especifica perfil, usar el personal
            perfil = usuario.getPerfilPersonal();
            if (perfil == null) {
                throw new CustomException("No se encontró un perfil para el movimiento", HttpStatus.BAD_REQUEST);
            }
        }

        Movimiento movimiento = Movimiento.builder()
                .monto(request.getMonto())
                .tipo(request.getTipo())
                .categoria(request.getCategoria())
                .descripcion(request.getDescripcion())
                .fecha(request.getFecha() != null ? request.getFecha() : LocalDate.now())
                .perfil(perfil)
                .usuario(usuario)
                .grupo(usuario.getGrupo())
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();

        Movimiento savedMovimiento = movimientoRepository.save(movimiento);
        log.info("✅ Movimiento creado: {} - Perfil: {}", savedMovimiento.getId(), perfil.getNombreCompleto());

        return savedMovimiento;
    }

    /**
     * Validar acceso a perfil
     */
    private void validarAccesoAPerfil(Usuarios usuario, Perfil perfil) {
        if (usuario.esAdministradorDePerfiles()) {
            if (!perfil.getUsuarioAdministrador().getId().equals(usuario.getId())) {
                throw new CustomException("No tienes permiso para usar este perfil", HttpStatus.FORBIDDEN);
            }
            return;
        }

        if (usuario.esPerfilIndependiente()) {
            if (usuario.getPerfilAsociado() == null ||
                    !usuario.getPerfilAsociado().getId().equals(perfil.getId())) {
                throw new CustomException("No tienes permiso para usar este perfil", HttpStatus.FORBIDDEN);
            }
            return;
        }

        throw new CustomException("Tipo de usuario no válido", HttpStatus.FORBIDDEN);
    }

    /**
     * Guardar movimiento (método simple)
     */
    public Movimiento guardar(Movimiento movimiento) {
        return movimientoRepository.save(movimiento);
    }

    /**
     * Obtener movimientos de un perfil
     */
    public Page<Movimiento> getMovimientosByPerfil(Long perfilId, Pageable pageable) {
        return movimientoRepository.findByPerfilIdAndActivoTrue(perfilId, pageable);
    }

    /**
     * Obtener movimientos de un usuario
     */
    public Page<Movimiento> getMovimientosByUsuario(Long usuarioId, Pageable pageable) {
        return movimientoRepository.findByUsuarioIdAndActivoTrue(usuarioId, pageable);
    }

    /**
     * Obtener movimientos de un administrador (todos sus perfiles)
     */
    public Page<Movimiento> getMovimientosByAdministrador(Long administradorId, Pageable pageable) {
        return movimientoRepository.findByUsuarioAdministradorIdAndActivoTrue(administradorId, pageable);
    }

    /**
     * Obtener movimientos de un grupo
     */
    public Page<Movimiento> getMovimientosByGrupo(Long grupoId, Pageable pageable) {
        return movimientoRepository.findByGrupoIdAndActivoTrue(grupoId, pageable);
    }

    /**
     * Listar movimientos por usuario (método simple)
     */
    public List<Movimiento> listarPorUsuario(Long idUsuario) {
        return movimientoRepository.findByUsuarioId(idUsuario);
    }

    /**
     * Obtener movimientos por rango de fechas
     */
    public List<Movimiento> getMovimientosByFecha(Long perfilId, LocalDate inicio, LocalDate fin) {
        return movimientoRepository.findByPerfilIdAndFechaBetweenAndActivoTrue(perfilId, inicio, fin);
    }

    /**
     * Obtener movimiento por ID
     */
    public Movimiento getMovimientoById(Long id) {
        return movimientoRepository.findById(id)
                .orElseThrow(() -> new CustomException("Movimiento no encontrado con ID: " + id, HttpStatus.NOT_FOUND));
    }

    /**
     * Actualizar movimiento
     */
    public Movimiento actualizarMovimiento(Long id, MovimientoRequest request, Long usuarioId) {
        Movimiento movimiento = getMovimientoById(id);

        // Validar que el usuario sea el dueño del movimiento
        if (!movimiento.getUsuario().getId().equals(usuarioId)) {
            throw new CustomException("No tienes permiso para modificar este movimiento", HttpStatus.FORBIDDEN);
        }

        movimiento.setMonto(request.getMonto());
        movimiento.setTipo(request.getTipo());
        movimiento.setCategoria(request.getCategoria());
        movimiento.setDescripcion(request.getDescripcion());
        movimiento.setFecha(request.getFecha() != null ? request.getFecha() : LocalDate.now());
        movimiento.setFechaActualizacion(LocalDateTime.now());

        Movimiento updatedMovimiento = movimientoRepository.save(movimiento);
        log.info("✅ Movimiento actualizado: {}", updatedMovimiento.getId());

        return updatedMovimiento;
    }

    /**
     * Eliminar movimiento (desactivar)
     */
    public void eliminarMovimiento(Long id, Long usuarioId) {
        Movimiento movimiento = getMovimientoById(id);

        // Validar que el usuario sea el dueño del movimiento
        if (!movimiento.getUsuario().getId().equals(usuarioId)) {
            throw new CustomException("No tienes permiso para eliminar este movimiento", HttpStatus.FORBIDDEN);
        }

        movimiento.setActivo(false);
        movimiento.setFechaActualizacion(LocalDateTime.now());
        movimientoRepository.save(movimiento);

        log.info("🗑️ Movimiento eliminado: {}", id);
    }

    /**
     * Calcular total por tipo
     */
    public BigDecimal calcularTotalPorTipo(List<Movimiento> movimientos, TipoMovimiento tipo) {
        return movimientos.stream()
                .filter(m -> m.getTipo() == tipo)
                .map(Movimiento::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcular balance
     */
    public BigDecimal calcularBalance(List<Movimiento> movimientos) {
        BigDecimal ingresos = calcularTotalPorTipo(movimientos, TipoMovimiento.INGRESO);
        BigDecimal gastos = calcularTotalPorTipo(movimientos, TipoMovimiento.GASTO);
        return ingresos.subtract(gastos);
    }

    /**
     * Agrupar por categoría
     */
    public Map<String, BigDecimal> agruparPorCategoria(List<Movimiento> movimientos) {
        return movimientos.stream()
                .filter(m -> m.getTipo() == TipoMovimiento.GASTO)
                .collect(Collectors.groupingBy(
                        m -> m.getCategoria().getDescripcion(),
                        Collectors.mapping(Movimiento::getMonto, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));
    }

    /**
     * Obtener resumen mensual de un perfil
     */
    public ResumenMensualResponse getResumenMensualPerfil(Long perfilId, YearMonth mes) {
        Perfil perfil = perfilRepository.findById(perfilId)
                .orElseThrow(() -> new CustomException("Perfil no encontrado", HttpStatus.NOT_FOUND));

        LocalDate inicio = mes.atDay(1);
        LocalDate fin = mes.atEndOfMonth();

        List<Movimiento> movimientos = movimientoRepository.findByPerfilIdAndFechaBetweenAndActivoTrue(
                perfilId, inicio, fin
        );

        BigDecimal ingresos = calcularTotalPorTipo(movimientos, TipoMovimiento.INGRESO);
        BigDecimal gastos = calcularTotalPorTipo(movimientos, TipoMovimiento.GASTO);

        return ResumenMensualResponse.builder()
                .mes(mes)
                .totalIngresos(ingresos)
                .totalGastos(gastos)
                .balance(ingresos.subtract(gastos))
                .gastosPorCategoria(agruparPorCategoria(movimientos))
                .build();
    }
}




