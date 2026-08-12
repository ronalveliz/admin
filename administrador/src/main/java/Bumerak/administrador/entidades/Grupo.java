package Bumerak.administrador.entidades;

import Bumerak.administrador.entidades.enums.TipoGrupo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "grupos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Grupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoGrupo tipo;

    // ========== RELACIONES ==========

    /**
     * Usuario que administra este grupo (Juan)
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrador_id", nullable = false)
    private Usuarios administrador;

    /**
     * Usuarios que pertenecen a este grupo
     * Incluye al administrador y a los perfiles independientes
     */
    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Usuarios> usuarios = new ArrayList<>();

    /**
     * Perfiles que pertenecen a este grupo
     */
    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Perfil> perfiles = new ArrayList<>();

    /**
     * Movimientos del grupo
     */
    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Movimiento> movimientos = new ArrayList<>();

    // ========== CAMPOS DE AUDITORÍA ==========

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // ========== MÉTODOS DE UTILIDAD ==========

    public boolean esFamilia() {
        return this.tipo == TipoGrupo.FAMILIA;
    }

    public boolean esEmpresa() {
        return this.tipo == TipoGrupo.EMPRESA;
    }

    public boolean esAdministrador(Usuarios usuario) {
        return this.administrador != null &&
                this.administrador.getId().equals(usuario.getId());
    }

    public boolean contieneUsuario(Usuarios usuario) {
        return this.usuarios.stream()
                .anyMatch(u -> u.getId().equals(usuario.getId()));
    }

    public boolean contienePerfil(Perfil perfil) {
        return this.perfiles.stream()
                .anyMatch(p -> p.getId().equals(perfil.getId()));
    }

    public void agregarUsuario(Usuarios usuario) {
        if (!this.usuarios.contains(usuario)) {
            this.usuarios.add(usuario);
            usuario.setGrupo(this);
        }
    }

    public void agregarPerfil(Perfil perfil) {
        if (!this.perfiles.contains(perfil)) {
            this.perfiles.add(perfil);
            perfil.setGrupo(this);
        }
    }
}