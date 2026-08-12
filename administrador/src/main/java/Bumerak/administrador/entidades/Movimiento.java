package Bumerak.administrador.entidades;

import Bumerak.administrador.entidades.enums.CategoriaMovimiento;
import Bumerak.administrador.entidades.enums.TipoMovimiento;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Movimiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimiento tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaMovimiento categoria;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(length = 500)
    private String comprobanteUrl;

    // ========== RELACIONES ==========

    /**
     * Perfil al que pertenece este movimiento
     * Un movimiento SIEMPRE tiene un perfil asociado
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_id", nullable = false)
    private Perfil perfil;

    /**
     * Usuario que creó este movimiento
     * Puede ser el administrador (Juan) o el perfil independiente (María)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuarios usuario;

    /**
     * Grupo al que pertenece este movimiento
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    // ========== CAMPOS DE AUDITORÍA ==========

    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // ========== MÉTODOS DE UTILIDAD ==========

    public boolean esIngreso() {
        return this.tipo == TipoMovimiento.INGRESO;
    }

    public boolean esGasto() {
        return this.tipo == TipoMovimiento.GASTO;
    }

    public boolean esTransferencia() {
        return this.tipo == TipoMovimiento.TRANSFERENCIA;
    }
}



