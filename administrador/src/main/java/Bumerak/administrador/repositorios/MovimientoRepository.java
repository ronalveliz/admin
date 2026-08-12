package Bumerak.administrador.repositorios;

import Bumerak.administrador.entidades.Movimiento;
import Bumerak.administrador.entidades.enums.CategoriaMovimiento;
import Bumerak.administrador.entidades.enums.TipoMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

// ========== BÚSQUEDAS BÁSICAS ==========

    /**
     * Buscar movimientos por tipo
     */
    List<Movimiento> findByTipo(TipoMovimiento tipo);

    /**
     * Buscar movimientos por tipo con paginación
     */
    Page<Movimiento> findByTipo(TipoMovimiento tipo, Pageable pageable);

    /**
     * Buscar movimientos por categoría
     */
    List<Movimiento> findByCategoria(CategoriaMovimiento categoria);

    /**
     * Buscar movimientos por fecha
     */
    List<Movimiento> findByFecha(LocalDate fecha);

    /**
     * Buscar movimientos entre fechas
     */
    List<Movimiento> findByFechaBetween(LocalDate fechaInicio, LocalDate fechaFin);

    // ========== BÚSQUEDAS POR PERFIL ==========

    /**
     * Buscar movimientos de un perfil
     */
    List<Movimiento> findByPerfilId(Long perfilId);

    /**
     * Buscar movimientos de un perfil con paginación
     */
    Page<Movimiento> findByPerfilId(Long perfilId, Pageable pageable);

    /**
     * Buscar movimientos activos de un perfil
     */
    List<Movimiento> findByPerfilIdAndActivoTrue(Long perfilId);

    /**
     * Buscar movimientos activos de un perfil con paginación
     */
    Page<Movimiento> findByPerfilIdAndActivoTrue(Long perfilId, Pageable pageable);

    /**
     * Buscar movimientos de un perfil entre fechas
     */
    List<Movimiento> findByPerfilIdAndFechaBetweenAndActivoTrue(
            Long perfilId,
            LocalDate fechaInicio,
            LocalDate fechaFin
    );

    /**
     * Buscar movimientos de un perfil por tipo
     */
    List<Movimiento> findByPerfilIdAndTipoAndActivoTrue(
            Long perfilId,
            TipoMovimiento tipo
    );

    // ========== BÚSQUEDAS POR USUARIO ==========

    /**
     * Buscar movimientos creados por un usuario
     */
    List<Movimiento> findByUsuarioId(Long usuarioId);

    /**
     * Buscar movimientos creados por un usuario con paginación
     */
    Page<Movimiento> findByUsuarioId(Long usuarioId, Pageable pageable);

    /**
     * Buscar movimientos activos creados por un usuario
     */
    List<Movimiento> findByUsuarioIdAndActivoTrue(Long usuarioId);

    /**
     * Buscar movimientos activos creados por un usuario con paginación
     */
    Page<Movimiento> findByUsuarioIdAndActivoTrue(Long usuarioId, Pageable pageable);

    /**
     * Buscar movimientos creados por el administrador de perfiles
     */
    @Query("SELECT m FROM Movimiento m " +
            "JOIN m.perfil p " +
            "WHERE p.usuarioAdministrador.id = :administradorId " +
            "AND m.activo = true")
    Page<Movimiento> findByUsuarioAdministradorIdAndActivoTrue(
            @Param("administradorId") Long administradorId,
            Pageable pageable
    );

    // ========== BÚSQUEDAS POR GRUPO ==========

    /**
     * Buscar movimientos de un grupo
     */
    List<Movimiento> findByGrupoId(Long grupoId);

    /**
     * Buscar movimientos de un grupo con paginación
     */
    Page<Movimiento> findByGrupoId(Long grupoId, Pageable pageable);

    /**
     * Buscar movimientos activos de un grupo
     */
    List<Movimiento> findByGrupoIdAndActivoTrue(Long grupoId);

    /**
     * Buscar movimientos activos de un grupo con paginación
     */
    Page<Movimiento> findByGrupoIdAndActivoTrue(Long grupoId, Pageable pageable);

    /**
     * Buscar movimientos de un grupo entre fechas
     */
    List<Movimiento> findByGrupoIdAndFechaBetweenAndActivoTrue(
            Long grupoId,
            LocalDate fechaInicio,
            LocalDate fechaFin
    );

    // ========== BÚSQUEDAS AVANZADAS ==========

    /**
     * Buscar movimientos por rango de fechas con paginación
     */
    Page<Movimiento> findByFechaBetween(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Pageable pageable
    );

    /**
     * Buscar movimientos por descripción (contiene)
     */
    List<Movimiento> findByDescripcionContainingIgnoreCase(String descripcion);

    /**
     * Buscar movimientos por monto mayor a
     */
    List<Movimiento> findByMontoGreaterThan(BigDecimal monto);

    /**
     * Buscar movimientos por monto menor a
     */
    List<Movimiento> findByMontoLessThan(BigDecimal monto);

    // ========== CONSULTAS DE AGREGACIÓN ==========

    /**
     * Sumar montos por tipo de movimiento en un perfil
     */
    @Query("SELECT SUM(m.monto) FROM Movimiento m " +
            "WHERE m.perfil.id = :perfilId " +
            "AND m.tipo = :tipo " +
            "AND m.activo = true")
    BigDecimal sumMontoByPerfilIdAndTipo(
            @Param("perfilId") Long perfilId,
            @Param("tipo") TipoMovimiento tipo
    );

    /**
     * Sumar montos por tipo de movimiento en un grupo
     */
    @Query("SELECT SUM(m.monto) FROM Movimiento m " +
            "WHERE m.grupo.id = :grupoId " +
            "AND m.tipo = :tipo " +
            "AND m.activo = true")
    BigDecimal sumMontoByGrupoIdAndTipo(
            @Param("grupoId") Long grupoId,
            @Param("tipo") TipoMovimiento tipo
    );

    /**
     * Sumar montos por tipo de movimiento y categoría en un grupo
     */
    @Query("SELECT m.categoria, SUM(m.monto) FROM Movimiento m " +
            "WHERE m.grupo.id = :grupoId " +
            "AND m.tipo = :tipo " +
            "AND m.activo = true " +
            "GROUP BY m.categoria")
    List<Object[]> sumMontoByGrupoIdAndTipoGroupByCategoria(
            @Param("grupoId") Long grupoId,
            @Param("tipo") TipoMovimiento tipo
    );

    /**
     * Obtener movimientos de un perfil agrupados por mes
     */
    @Query("SELECT FUNCTION('DATE_FORMAT', m.fecha, '%Y-%m') as mes, " +
            "SUM(CASE WHEN m.tipo = 'INGRESO' THEN m.monto ELSE 0 END) as ingresos, " +
            "SUM(CASE WHEN m.tipo = 'GASTO' THEN m.monto ELSE 0 END) as gastos " +
            "FROM Movimiento m " +
            "WHERE m.perfil.id = :perfilId " +
            "AND m.activo = true " +
            "GROUP BY FUNCTION('DATE_FORMAT', m.fecha, '%Y-%m') " +
            "ORDER BY mes DESC")
    List<Object[]> getResumenMensualByPerfilId(@Param("perfilId") Long perfilId);

    /**
     * Obtener movimientos de un grupo agrupados por mes
     */
    @Query(value = "SELECT DATE_FORMAT(m.fecha, '%Y-%m') as mes, " +
            "SUM(CASE WHEN m.tipo = 'INGRESO' THEN m.monto ELSE 0 END) as ingresos, " +
            "SUM(CASE WHEN m.tipo = 'GASTO' THEN m.monto ELSE 0 END) as gastos " +
            "FROM movimientos m " +
            "WHERE m.grupo_id = :grupoId " +
            "AND m.activo = true " +
            "GROUP BY DATE_FORMAT(m.fecha, '%Y-%m') " +
            "ORDER BY mes DESC", nativeQuery = true)
    List<Object[]> getResumenMensualByGrupoId(@Param("grupoId") Long grupoId);

    /**
     * Contar movimientos por tipo en un perfil
     */
    long countByPerfilIdAndTipoAndActivoTrue(Long perfilId, TipoMovimiento tipo);

    /**
     * Contar movimientos por categoría en un grupo
     */
    @Query("SELECT COUNT(m) FROM Movimiento m " +
            "WHERE m.grupo.id = :grupoId " +
            "AND m.categoria = :categoria " +
            "AND m.activo = true")
    long countByGrupoIdAndCategoriaAndActivoTrue(
            @Param("grupoId") Long grupoId,
            @Param("categoria") CategoriaMovimiento categoria
    );
}