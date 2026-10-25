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
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio encargado de la lógica de negocio relacionada con los movimientos financieros.
 * <p>
 * Proporciona operaciones para crear, consultar, actualizar, eliminar y calcular
 * resúmenes o estadísticas de los movimientos (ingresos, gastos y transferencias).
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MovimientoService {

    private final MovimientoRepository movimientoRepository;
    private final UsuariosRepository usuariosRepository;
    private final PerfilRepository perfilRepository;

    // ================================================================
    // CREACIÓN
    // ================================================================

    /**
     * Crea un nuevo movimiento financiero para un usuario.
     * <p>
     * Lógica de asignación de perfil:
     * <ul>
     *     <li>Si el request incluye un {@code perfilId}, se valida que el usuario
     *         tenga permisos sobre ese perfil y se asigna a él.</li>
     *     <li>Si no se incluye {@code perfilId}:
     *         <ul>
     *             <li>Para un <b>perfil independiente</b> (MIEMBRO/EMPLEADO), se asigna
     *                 automáticamente a su {@code perfilAsociado}.</li>
     *             <li>Para un <b>administrador</b> (FAMILIA/EMPRESA/ADMINISTRADOR), se asigna
     *                 automáticamente a su perfil personal.</li>
     *         </ul>
     *     </li>
     * </ul>
     * </p>
     *
     * @param request   Datos del movimiento (monto, tipo, categoría, fecha, etc.).
     * @param usuarioId ID del usuario autenticado que crea el movimiento.
     * @return El movimiento guardado en la base de datos.
     * @throws CustomException si el usuario no existe, el perfil no se encuentra,
     *                         o el usuario no tiene permiso sobre el perfil indicado.
     */
    public Movimiento crearMovimiento(MovimientoRequest request, Long usuarioId) {
        // 1. Buscar al usuario creador
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new CustomException("Usuario no encontrado", HttpStatus.NOT_FOUND));

        Perfil perfil;

        // 2. Determinar el perfil destino
        if (request.getPerfilId() != null) {
            // Caso A: Se especifica un perfil explícito -> validar que tenga acceso
            perfil = perfilRepository.findById(request.getPerfilId())
                    .orElseThrow(() -> new CustomException("Perfil no encontrado", HttpStatus.NOT_FOUND));

            validarAccesoAPerfil(usuario, perfil);

        } else {
            // Caso B: Sin perfilId -> asignar según el rol del usuario
            if (usuario.esPerfilIndependiente()) {
                perfil = usuario.getPerfilAsociado(); // MIEMBRO / EMPLEADO
            } else {
                perfil = usuario.getPerfilPersonal(); // FAMILIA / EMPRESA / ADMIN
            }

            if (perfil == null) {
                throw new CustomException(
                        "No se encontró un perfil para asignar el movimiento",
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        // 3. Construir y guardar la entidad Movimiento
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
        log.info("✅ Movimiento creado: {} - Perfil: {}",
                savedMovimiento.getId(),
                perfil.getNombreCompleto());

        return savedMovimiento;
    }

    // ================================================================
    // VALIDACIONES
    // ================================================================

    /**
     * Valida que un usuario tenga acceso al perfil indicado.
     * <p>
     * Reglas:
     * <ul>
     *     <li>Administrador: solo si es el propietario del perfil ({@code usuarioAdministrador}).</li>
     *     <li>Perfil independiente: solo si el perfil es su {@code perfilAsociado}.</li>
     * </ul>
     * </p>
     *
     * @param usuario Usuario autenticado.
     * @param perfil  Perfil al que se quiere acceder.
     * @throws CustomException si el usuario no tiene permiso o su tipo no es válido.
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

    // ================================================================
    // GUARDADO Y CONSULTAS
    // ================================================================

    /**
     * Guarda o actualiza un movimiento directamente en la base de datos.
     * Útil para flujos simples donde la entidad ya está construida.
     *
     * @param movimiento Entidad {@link Movimiento} a persistir.
     * @return La entidad guardada.
     */
    public Movimiento guardar(Movimiento movimiento) {
        return movimientoRepository.save(movimiento);
    }

    /**
     * Obtiene una página con los movimientos activos de un perfil concreto.
     *
     * @param perfilId ID del perfil.
     * @param pageable Parámetros de paginación y ordenación.
     * @return Página de movimientos.
     */
    public Page<Movimiento> getMovimientosByPerfil(Long perfilId, Pageable pageable) {
        return movimientoRepository.findByPerfilIdAndActivoTrue(perfilId, pageable);
    }

    /**
     * Obtiene una página con los movimientos activos creados por un usuario.
     * Aplicable a perfiles independientes (ven solo los suyos).
     *
     * @param usuarioId ID del usuario creador.
     * @param pageable  Parámetros de paginación.
     * @return Página de movimientos.
     */
    public Page<Movimiento> getMovimientosByUsuario(Long usuarioId, Pageable pageable) {
        return movimientoRepository.findByUsuarioIdAndActivoTrue(usuarioId, pageable);
    }

    /**
     * Obtiene una página con todos los movimientos activos de los perfiles
     * administrados por un administrador (FAMILIA, EMPRESA o ADMIN).
     *
     * @param administradorId ID del usuario administrador.
     * @param pageable        Parámetros de paginación.
     * @return Página de movimientos.
     */
    public Page<Movimiento> getMovimientosByAdministrador(Long administradorId, Pageable pageable) {
        return movimientoRepository.findByUsuarioAdministradorIdAndActivoTrue(administradorId, pageable);
    }

    /**
     * Obtiene los movimientos activos de un grupo específico.
     *
     * @param grupoId  ID del grupo.
     * @param pageable Parámetros de paginación.
     * @return Página de movimientos.
     */
    public Page<Movimiento> getMovimientosByGrupo(Long grupoId, Pageable pageable) {
        return movimientoRepository.findByGrupoIdAndActivoTrue(grupoId, pageable);
    }

    /**
     * Lista todos los movimientos (activos e inactivos) creados por un usuario.
     *
     * @param idUsuario ID del usuario.
     * @return Lista de movimientos.
     */
    public List<Movimiento> listarPorUsuario(Long idUsuario) {
        return movimientoRepository.findByUsuarioId(idUsuario);
    }

    /**
     * Obtiene los movimientos activos de un perfil dentro de un rango de fechas.
     *
     * @param perfilId ID del perfil.
     * @param inicio   Fecha de inicio (inclusive).
     * @param fin      Fecha de fin (inclusive).
     * @return Lista de movimientos.
     */
    public List<Movimiento> getMovimientosByFecha(Long perfilId, LocalDate inicio, LocalDate fin) {
        return movimientoRepository.findByPerfilIdAndFechaBetweenAndActivoTrue(perfilId, inicio, fin);
    }

    /**
     * Busca un movimiento por su ID.
     *
     * @param id ID del movimiento.
     * @return El movimiento encontrado.
     * @throws CustomException si el movimiento no existe.
     */
    public Movimiento getMovimientoById(Long id) {
        return movimientoRepository.findById(id)
                .orElseThrow(() -> new CustomException("Movimiento no encontrado con ID: " + id, HttpStatus.NOT_FOUND));
    }

    // ================================================================
    // ACTUALIZACIÓN Y ELIMINACIÓN
    // ================================================================

    /**
     * Actualiza los datos de un movimiento existente.
     * Solo el usuario que creó el movimiento puede modificarlo.
     *
     * @param id        ID del movimiento a actualizar.
     * @param request   Nuevos datos del movimiento.
     * @param usuarioId ID del usuario que intenta actualizar.
     * @return El movimiento actualizado.
     * @throws CustomException si el movimiento no existe o el usuario no tiene permiso.
     */
    public Movimiento actualizarMovimiento(Long id, MovimientoRequest request, Long usuarioId) {
        Movimiento movimiento = getMovimientoById(id);

        // Verificar que el usuario sea el propietario
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
     * Elimina lógicamente un movimiento (lo marca como inactivo).
     * Solo el creador del movimiento puede eliminarlo.
     *
     * @param id        ID del movimiento.
     * @param usuarioId ID del usuario que intenta eliminar.
     * @throws CustomException si el movimiento no existe o el usuario no tiene permiso.
     */
    public void eliminarMovimiento(Long id, Long usuarioId) {
        Movimiento movimiento = getMovimientoById(id);

        // Verificar que el usuario sea el propietario
        if (!movimiento.getUsuario().getId().equals(usuarioId)) {
            throw new CustomException("No tienes permiso para eliminar este movimiento", HttpStatus.FORBIDDEN);
        }

        movimiento.setActivo(false);
        movimiento.setFechaActualizacion(LocalDateTime.now());
        movimientoRepository.save(movimiento);

        log.info("🗑️ Movimiento eliminado: {}", id);
    }

    // ================================================================
    // CÁLCULOS Y ESTADÍSTICAS
    // ================================================================

    /**
     * Calcula la suma total de los montos de una lista de movimientos filtrados por tipo.
     *
     * @param movimientos Lista de movimientos.
     * @param tipo        Tipo de movimiento (INGRESO, GASTO, TRANSFERENCIA).
     * @return Suma total como {@link BigDecimal}. Cero si no hay coincidencias.
     */
    public BigDecimal calcularTotalPorTipo(List<Movimiento> movimientos, TipoMovimiento tipo) {
        return movimientos.stream()
                .filter(m -> m.getTipo() == tipo)
                .map(Movimiento::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcula el balance total (ingresos - gastos) de una lista de movimientos.
     *
     * @param movimientos Lista de movimientos.
     * @return Balance como {@link BigDecimal}.
     */
    public BigDecimal calcularBalance(List<Movimiento> movimientos) {
        BigDecimal ingresos = calcularTotalPorTipo(movimientos, TipoMovimiento.INGRESO);
        BigDecimal gastos = calcularTotalPorTipo(movimientos, TipoMovimiento.GASTO);
        return ingresos.subtract(gastos);
    }

    /**
     * Agrupa los gastos por categoría y calcula el total gastado en cada una.
     *
     * @param movimientos Lista de movimientos.
     * @return Mapa donde la clave es el nombre de la categoría y el valor es el total gastado.
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
     * Genera un resumen mensual de un perfil concreto: totales de ingresos, gastos,
     * balance y desglose de gastos por categoría.
     *
     * @param perfilId ID del perfil.
     * @param mes      Mes y año ({@link YearMonth}) sobre el que se calcula el resumen.
     * @return Objeto {@link ResumenMensualResponse} con las estadísticas del mes.
     * @throws CustomException si el perfil no existe.
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