package Bumerak.administrador.servicios;

import Bumerak.administrador.dto.response.ResumenMensualResponse;
import Bumerak.administrador.dto.response.PerfilResumenResponse;
import Bumerak.administrador.entidades.Grupo;
import Bumerak.administrador.entidades.Movimiento;
import Bumerak.administrador.entidades.Usuarios;
import Bumerak.administrador.entidades.enums.TipoGrupo;
import Bumerak.administrador.entidades.enums.TipoMovimiento;
import Bumerak.administrador.entidades.enums.TipoPerfil;
import Bumerak.administrador.exception.CustomException;
import Bumerak.administrador.repositorios.GrupoRepository;
import Bumerak.administrador.repositorios.MovimientoRepository;
import Bumerak.administrador.repositorios.PerfilRepository;
import Bumerak.administrador.repositorios.UsuariosRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
public class GrupoService {

    private final GrupoRepository grupoRepository;
    private final UsuariosRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final MovimientoRepository movimientoRepository;

    // ========== MÉTODOS PÚBLICOS ==========

    /**
     * Obtener grupo por ID
     */
    public Grupo getGrupoById(Long id) {
        return grupoRepository.findById(id)
                .orElseThrow(() -> new CustomException("Grupo no encontrado con ID: " + id));
    }

    /**
     * Obtener grupo completo
     */
    public Grupo getGrupoCompleto(Long id) {
        return grupoRepository.findByIdCompleto(id)
                .orElseThrow(() -> new CustomException("Grupo no encontrado con ID: " + id));
    }

    /**
     * Obtener grupo de un usuario
     */
    public Grupo getGrupoByUsuario(Long usuarioId) {
        Usuarios usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new CustomException("Usuario no encontrado"));

        if (usuario.getGrupo() == null) {
            throw new CustomException("El usuario no pertenece a ningún grupo");
        }

        return usuario.getGrupo();
    }

    /**
     * Actualizar grupo
     */
    public Grupo actualizarGrupo(Long id, String nombre, String descripcion) {
        Grupo grupo = getGrupoById(id);

        grupo.setNombre(nombre);
        grupo.setDescripcion(descripcion);
        grupo.setFechaActualizacion(LocalDateTime.now());

        Grupo updatedGrupo = grupoRepository.save(grupo);
        log.info("✅ Grupo actualizado: {}", updatedGrupo.getNombre());

        return updatedGrupo;
    }

    /**
     * Deshabilitar grupo
     */
    public void deshabilitarGrupo(Long id) {
        Grupo grupo = getGrupoById(id);
        grupo.setActivo(false);
        grupo.setFechaActualizacion(LocalDateTime.now());
        grupoRepository.save(grupo);

        log.info("⚠️ Grupo deshabilitado: {}", grupo.getNombre());
    }

    /**
     * Obtener resumen mensual del grupo
     */
    public ResumenMensualResponse getResumenMensualGrupo(Long grupoId, YearMonth mes, Long usuarioId) {
        Grupo grupo = getGrupoById(grupoId);
        Usuarios usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new CustomException("Usuario no encontrado"));

        // Validar acceso
        validarAccesoAGrupo(grupo, usuario);

        LocalDate inicio = mes.atDay(1);
        LocalDate fin = mes.atEndOfMonth();

        // Obtener movimientos del grupo
        List<Movimiento> movimientos = movimientoRepository.findByGrupoIdAndFechaBetweenAndActivoTrue(
                grupoId, inicio, fin
        );

        // Calcular totales
        BigDecimal totalIngresos = calcularTotalPorTipo(movimientos, TipoMovimiento.INGRESO);
        BigDecimal totalGastos = calcularTotalPorTipo(movimientos, TipoMovimiento.GASTO);

        // ✅ Resumen por perfil - Usando la clase independiente
        List<PerfilResumenResponse> resumenPorPerfil = perfilRepository.findByGrupoIdAndActivoTrue(grupoId)
                .stream()
                .map(perfil -> {
                    List<Movimiento> movsPerfil = movimientos.stream()
                            .filter(m -> m.getPerfil() != null &&
                                    m.getPerfil().getId().equals(perfil.getId()))
                            .collect(Collectors.toList());

                    return PerfilResumenResponse.builder()
                            .perfilId(perfil.getId())
                            .perfilNombre(perfil.getNombreCompleto())
                            .perfilTipo(perfil.getTipo())
                            .ingresos(calcularTotalPorTipo(movsPerfil, TipoMovimiento.INGRESO))
                            .gastos(calcularTotalPorTipo(movsPerfil, TipoMovimiento.GASTO))
                            .balance(calcularBalance(movsPerfil))
                            .cantidadMovimientos(movsPerfil.size())
                            .build();
                })
                .collect(Collectors.toList());

        // Gastos por categoría
        Map<String, BigDecimal> gastosPorCategoria = movimientos.stream()
                .filter(m -> m.getTipo() == TipoMovimiento.GASTO)
                .collect(Collectors.groupingBy(
                        m -> m.getCategoria().getDescripcion(),
                        Collectors.mapping(Movimiento::getMonto, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        return ResumenMensualResponse.builder()
                .mes(mes)
                .totalIngresos(totalIngresos)
                .totalGastos(totalGastos)
                .balance(totalIngresos.subtract(totalGastos))
                .gastosPorCategoria(gastosPorCategoria)
                .resumenPorPerfil(resumenPorPerfil)  // ✅ Ahora coincide el tipo
                .build();
    }

    /**
     * Validar acceso a grupo
     */
    public void validarAccesoAGrupo(Grupo grupo, Usuarios usuario) {
        if (usuario.esAdministrador()) {
            return;
        }

        if (grupo.esAdministrador(usuario)) {
            return;
        }

        if (grupo.contieneUsuario(usuario)) {
            return;
        }

        throw new CustomException("No tienes permiso para acceder a este grupo");
    }

    /**
     * Validar que el tipo de perfil sea compatible con el grupo
     */
    public void validarPerfilParaGrupo(TipoGrupo tipoGrupo, TipoPerfil tipoPerfil) {
        if (tipoGrupo == TipoGrupo.FAMILIA &&
                tipoPerfil != TipoPerfil.MIEMBRO &&
                tipoPerfil != TipoPerfil.PERSONAL) {
            throw new CustomException("En una familia solo se pueden agregar perfiles de tipo MIEMBRO o PERSONAL");
        }

        if (tipoGrupo == TipoGrupo.EMPRESA &&
                tipoPerfil != TipoPerfil.EMPLEADO &&
                tipoPerfil != TipoPerfil.PERSONAL) {
            throw new CustomException("En una empresa solo se pueden agregar perfiles de tipo EMPLEADO o PERSONAL");
        }
    }

    // ========== MÉTODOS PRIVADOS ==========

    /**
     * Calcular total por tipo de movimiento
     */
    private BigDecimal calcularTotalPorTipo(List<Movimiento> movimientos, TipoMovimiento tipo) {
        return movimientos.stream()
                .filter(m -> m.getTipo() == tipo)
                .map(Movimiento::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcular balance (ingresos - gastos)
     */
    private BigDecimal calcularBalance(List<Movimiento> movimientos) {
        BigDecimal ingresos = calcularTotalPorTipo(movimientos, TipoMovimiento.INGRESO);
        BigDecimal gastos = calcularTotalPorTipo(movimientos, TipoMovimiento.GASTO);
        return ingresos.subtract(gastos);
    }
}