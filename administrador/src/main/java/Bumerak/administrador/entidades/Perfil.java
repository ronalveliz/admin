package Bumerak.administrador.entidades;

import Bumerak.administrador.entidades.enums.TipoMovimiento;
import Bumerak.administrador.entidades.enums.TipoPerfil;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "perfiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Perfil {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 100)
    private String apellidos;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(length = 255)
    private String direccion;

    @Column(length = 255)
    private String fotoPerfil;

    private LocalDateTime fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPerfil tipo;

    // ========== RELACIONES ==========

    /**
     * Usuario administrador que creó/administra este perfil (SIEMPRE es Juan)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_administrador_id", nullable = false)
    private Usuarios usuarioAdministrador;

    /**
     * Usuario asociado para acceso independiente (María o Carlos)
     * Puede ser null si el perfil no tiene acceso independiente
     */
    @OneToOne(mappedBy = "perfilAsociado", fetch = FetchType.LAZY)
    private Usuarios usuarioAsociado;

    /**
     * Grupo al que pertenece este perfil
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    /**
     * Movimientos asociados a este perfil
     */
    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Movimiento> movimientos = new ArrayList<>();

    // ========== CAMPOS DE CONFIGURACIÓN ==========

    @Builder.Default
    private Boolean activo = true;

    @Builder.Default
    @Column(name = "tiene_acceso_independiente")
    private Boolean tieneAccesoIndependiente = false;

    // ========== CAMPOS DE AUDITORÍA ==========

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // ========== MÉTODOS DE UTILIDAD ==========

    public String getNombreCompleto() {
        if (this.apellidos != null && !this.apellidos.isEmpty()) {
            return this.nombre + " " + this.apellidos;
        }
        return this.nombre;
    }

    public boolean esPersonal() {
        return this.tipo == TipoPerfil.PERSONAL;
    }

    public boolean esMiembro() {
        return this.tipo == TipoPerfil.MIEMBRO;
    }

    public boolean esEmpleado() {
        return this.tipo == TipoPerfil.EMPLEADO;
    }

    public boolean tieneAccesoIndependiente() {
        return this.tieneAccesoIndependiente && this.usuarioAsociado != null;
    }

    // ========== MÉTODOS DE MOVIMIENTOS ==========

    public BigDecimal getTotalIngresos() {
        return this.movimientos.stream()
                .filter(m -> m.getTipo() == TipoMovimiento.INGRESO && m.getActivo())
                .map(Movimiento::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalGastos() {
        return this.movimientos.stream()
                .filter(m -> m.getTipo() == TipoMovimiento.GASTO && m.getActivo())
                .map(Movimiento::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getBalance() {
        return getTotalIngresos().subtract(getTotalGastos());
    }
}