package Bumerak.administrador.repositorios;

import Bumerak.administrador.entidades.Grupo;
import Bumerak.administrador.entidades.enums.TipoGrupo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {

    /**
     * Buscar grupo por nombre
     */
    Optional<Grupo> findByNombre(String nombre);

    /**
     * Buscar grupo por nombre (contiene)
     */
    List<Grupo> findByNombreContainingIgnoreCase(String nombre);

    /**
     * Buscar grupos por tipo
     */
    List<Grupo> findByTipo(TipoGrupo tipo);

    /**
     * Buscar grupos por tipo con paginación
     */
    Page<Grupo> findByTipo(TipoGrupo tipo, Pageable pageable);

    // ========== BÚSQUEDAS POR ADMINISTRADOR ==========

    /**
     * Buscar grupos administrados por un usuario
     */
    List<Grupo> findByAdministradorId(Long administradorId);

    /**
     * Buscar grupos administrados por un usuario con paginación
     */
    Page<Grupo> findByAdministradorId(Long administradorId, Pageable pageable);

    /**
     * Buscar grupo activo administrado por un usuario
     */
    Optional<Grupo> findByAdministradorIdAndActivoTrue(Long administradorId);

    // ========== BÚSQUEDAS AVANZADAS ==========

    /**
     * Buscar grupos activos
     */
    List<Grupo> findByActivoTrue();

    /**
     * Buscar grupos inactivos
     */
    List<Grupo> findByActivoFalse();

    /**
     * Buscar grupos por tipo y estado
     */
    List<Grupo> findByTipoAndActivoTrue(TipoGrupo tipo);

    // ========== CONSULTAS PERSONALIZADAS ==========

    /**
     * Obtener grupo con todos sus usuarios (evita N+1)
     */
    @Query("SELECT DISTINCT g FROM Grupo g " +
            "LEFT JOIN FETCH g.usuarios " +
            "WHERE g.id = :id")
    Optional<Grupo> findByIdWithUsuarios(@Param("id") Long id);

    /**
     * Obtener grupo con todos sus perfiles (evita N+1)
     */
    @Query("SELECT DISTINCT g FROM Grupo g " +
            "LEFT JOIN FETCH g.perfiles " +
            "WHERE g.id = :id")
    Optional<Grupo> findByIdWithPerfiles(@Param("id") Long id);

    /**
     * Obtener grupo con todos sus movimientos (evita N+1)
     */
    @Query("SELECT DISTINCT g FROM Grupo g " +
            "LEFT JOIN FETCH g.movimientos " +
            "WHERE g.id = :id")
    Optional<Grupo> findByIdWithMovimientos(@Param("id") Long id);

    /**
     * Obtener grupo completo (evita N+1)
     */
    @Query("SELECT DISTINCT g FROM Grupo g " +
            "LEFT JOIN FETCH g.usuarios " +
            "LEFT JOIN FETCH g.perfiles " +
            "LEFT JOIN FETCH g.administrador " +
            "WHERE g.id = :id")
    Optional<Grupo> findByIdCompleto(@Param("id") Long id);

    /**
     * Contar grupos por tipo
     */
    long countByTipo(TipoGrupo tipo);

    /**
     * Contar grupos activos por tipo
     */
    long countByTipoAndActivoTrue(TipoGrupo tipo);

    /**
     * Buscar grupos que contienen un usuario específico
     */
    @Query("SELECT g FROM Grupo g JOIN g.usuarios u WHERE u.id = :usuarioId")
    List<Grupo> findGruposByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * Buscar grupos que contienen un perfil específico
     */
    @Query("SELECT g FROM Grupo g JOIN g.perfiles p WHERE p.id = :perfilId")
    Optional<Grupo> findGrupoByPerfilId(@Param("perfilId") Long perfilId);
}
