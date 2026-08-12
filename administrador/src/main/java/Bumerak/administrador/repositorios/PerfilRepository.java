package Bumerak.administrador.repositorios;

import Bumerak.administrador.entidades.Perfil;
import Bumerak.administrador.entidades.enums.TipoPerfil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


    public interface PerfilRepository extends JpaRepository<Perfil, Long> {

        // ========== BÚSQUEDAS BÁSICAS ==========

        Optional<Perfil> findByEmail(String email);
        boolean existsByEmail(String email);
        List<Perfil> findByTipo(TipoPerfil tipo);
        Page<Perfil> findByTipo(TipoPerfil tipo, Pageable pageable);

        // ========== BÚSQUEDAS POR ADMINISTRADOR ==========

        List<Perfil> findByUsuarioAdministradorId(Long usuarioId);
        Page<Perfil> findByUsuarioAdministradorId(Long usuarioId, Pageable pageable);
        List<Perfil> findByUsuarioAdministradorIdAndActivoTrue(Long usuarioId);

        // ✅ AGREGAR ESTOS MÉTODOS
        long countByUsuarioAdministradorId(Long administradorId);
        long countByUsuarioAdministradorIdAndActivoTrue(Long administradorId);

        // ========== BÚSQUEDAS POR GRUPO ==========

        List<Perfil> findByGrupoId(Long grupoId);
        List<Perfil> findByGrupoIdAndTipo(Long grupoId, TipoPerfil tipo);
        List<Perfil> findByGrupoIdAndActivoTrue(Long grupoId);

        // ========== BÚSQUEDAS POR ACCESO INDEPENDIENTE ==========

        List<Perfil> findByTieneAccesoIndependienteTrue();
        List<Perfil> findByTieneAccesoIndependienteTrueAndActivoTrue();

        @Query("SELECT p FROM Perfil p WHERE p.usuarioAsociado.id = :usuarioId")
        Optional<Perfil> findByUsuarioAsociadoId(@Param("usuarioId") Long usuarioId);

        // ========== BÚSQUEDAS AVANZADAS ==========

        List<Perfil> findByNombreContainingIgnoreCase(String nombre);
        List<Perfil> findByActivoTrue();
        List<Perfil> findByActivoFalse();
        List<Perfil> findByNombreContainingIgnoreCaseAndTipo(String nombre, TipoPerfil tipo);

        // ========== CONSULTAS PERSONALIZADAS ==========

        @Query("SELECT DISTINCT p FROM Perfil p LEFT JOIN FETCH p.movimientos WHERE p.id = :id")
        Optional<Perfil> findByIdWithMovimientos(@Param("id") Long id);

        @Query("SELECT DISTINCT p FROM Perfil p LEFT JOIN FETCH p.usuarioAsociado WHERE p.id = :id")
        Optional<Perfil> findByIdWithUsuarioAsociado(@Param("id") Long id);

        @Query("SELECT DISTINCT p FROM Perfil p LEFT JOIN FETCH p.usuarioAdministrador LEFT JOIN FETCH p.grupo WHERE p.id = :id")
        Optional<Perfil> findByIdWithAdminAndGrupo(@Param("id") Long id);

        long countByGrupoIdAndTipo(Long grupoId, TipoPerfil tipo);
        long countByGrupoIdAndTipoAndActivoTrue(Long grupoId, TipoPerfil tipo);

        @Query("SELECT p FROM Perfil p WHERE p.grupo.id = :grupoId AND p.activo = true")
        Page<Perfil> findActivosByGrupoId(@Param("grupoId") Long grupoId, Pageable pageable);

        List<Perfil> findByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);
    }