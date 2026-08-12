package Bumerak.administrador.repositorios;

import Bumerak.administrador.entidades.Usuarios;
import Bumerak.administrador.entidades.enums.TipoRol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;

public interface UsuariosRepository extends JpaRepository<Usuarios, Long> {


    // ========== BÚSQUEDAS BÁSICAS ==========

    /**
     * Buscar usuario por email (para login)
     */
    Optional<Usuarios> findByEmail(String email);

    /**
     * Verificar si existe un usuario con ese email
     */
    boolean existsByEmail(String email);

    /**
     * Buscar usuarios por rol
     */
    List<Usuarios> findByRol(TipoRol rol);

    /**
     * Buscar usuarios por rol con paginación
     */
    Page<Usuarios> findByRol(TipoRol rol, Pageable pageable);

    // ========== BÚSQUEDAS POR GRUPO ==========

    /**
     * Buscar usuarios que pertenecen a un grupo
     */
    List<Usuarios> findByGrupoId(Long grupoId);

    /**
     * Buscar usuarios de un grupo con paginación
     */
    Page<Usuarios> findByGrupoId(Long grupoId, Pageable pageable);

    /**
     * Buscar administradores de un grupo
     */
    @Query("SELECT u FROM Usuarios u WHERE u.grupo.id = :grupoId AND u.rol IN :roles")
    List<Usuarios> findByGrupoIdAndRolIn(@Param("grupoId") Long grupoId,
                                        @Param("roles") List<TipoRol> roles);

    // ========== BÚSQUEDAS POR PERFIL ASOCIADO ==========

    /**
     * Buscar usuario asociado a un perfil
     */
    Optional<Usuarios> findByPerfilAsociadoId(Long perfilId);

    /**
     * Buscar usuarios que tienen perfil asociado (perfiles independientes)
     */
    List<Usuarios> findByPerfilAsociadoIsNotNull();

    // ========== BÚSQUEDAS AVANZADAS ==========

    /**
     * Buscar usuarios por nombre (contiene)
     */
    List<Usuarios> findByNombreContainingIgnoreCase(String nombre);

    /**
     * Buscar usuarios activos
     */
    List<Usuarios> findByEnabledTrue();

    /**
     * Buscar usuarios inactivos
     */
    List<Usuarios> findByEnabledFalse();

    /**
     * Buscar usuarios por email y rol
     */
    Optional<Usuarios> findByEmailAndRol(String email, TipoRol rol);

    // ========== CONSULTAS PERSONALIZADAS ==========

    /**
     * Obtener todos los usuarios con sus perfiles administrados
     * (Evita N+1)
     */
    @Query("SELECT DISTINCT u FROM Usuarios u " +
            "LEFT JOIN FETCH u.perfilesAdministrados " +
            "WHERE u.id = :id")
    Optional<Usuarios> findByIdWithPerfiles(@Param("id") Long id);

    /**
     * Obtener todos los usuarios con sus perfiles administrados y grupo
     */
    @Query("SELECT DISTINCT u FROM Usuarios u " +
            "LEFT JOIN FETCH u.perfilesAdministrados " +
            "LEFT JOIN FETCH u.grupo " +
            "WHERE u.id = :id")
    Optional<Usuarios> findByIdWithPerfilesAndGrupo(@Param("id") Long id);

    /**
     * Obtener todos los administradores de grupos (FAMILIA, EMPRESA, ADMINISTRADOR)
     */
    @Query("SELECT u FROM Usuarios u WHERE u.rol IN :roles")
    List<Usuarios> findAdministradores(@Param("roles") List<TipoRol> roles);

    /**
     * Contar usuarios por rol
     */
    long countByRol(TipoRol rol);

    /**
     * Contar usuarios activos por rol
     */
    long countByRolAndEnabledTrue(TipoRol rol);

    /**
     * Obtener usuarios que no tienen grupo
     */
    @Query("SELECT u FROM Usuarios u WHERE u.grupo IS NULL")
    List<Usuarios> findUsuariosSinGrupo();

    /**
     * Obtener usuarios por email con paginación (para búsquedas)
     */
    Page<Usuarios> findByEmailContainingIgnoreCase(String email, Pageable pageable);
}

