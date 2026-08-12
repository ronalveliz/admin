package Bumerak.administrador.entidades;

import Bumerak.administrador.entidades.enums.TipoPerfil;
import Bumerak.administrador.entidades.enums.TipoRol;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Usuarios {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoRol rol;

    @Column(length = 20)
    private String telefono;

    @Column(length = 255)
    private String direccion;

    @Column(length = 255)
    private String fotoPerfil;

    // ========== RELACIONES ==========

    /**
     * Grupo al que pertenece este usuario
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    /**
     * Perfiles que este usuario administra (solo si es ADMINISTRADOR, FAMILIA o EMPRESA)
     *tiene muchos perfiles
     */
    @OneToMany(mappedBy = "usuarioAdministrador", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Perfil> perfilesAdministrados = new ArrayList<>();

    /**
     * Perfil asociado a este usuario (solo si es MIEMBRO o EMPLEADO)
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_asociado_id")
    private Perfil perfilAsociado;

    /**
     * Movimientos creados por este usuario
     */
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Movimiento> movimientos = new ArrayList<>();

    // ========== CAMPOS DE SEGURIDAD ==========

    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    @Column(name = "account_non_locked")
    private Boolean accountNonLocked = true;

    @Builder.Default
    @Column(name = "account_non_expired")
    private Boolean accountNonExpired = true;

    @Builder.Default
    @Column(name = "credentials_non_expired")
    private Boolean credentialsNonExpired = true;

    // ========== CAMPOS DE AUDITORÍA ==========

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // ========== MÉTODOS DE UTILIDAD ==========

    public boolean esFamilia() {
        return this.rol == TipoRol.FAMILIA;
    }

    public boolean esEmpresa() {
        return this.rol == TipoRol.EMPRESA;
    }

    public boolean esAdministrador() {
        return this.rol == TipoRol.ADMINISTRADOR;
    }

    public boolean esMiembro() {
        return this.rol == TipoRol.MIEMBRO;
    }

    public boolean esEmpleado() {
        return this.rol == TipoRol.EMPLEADO;
    }

    public boolean esAdminDeGrupo() {
        return this.grupo != null &&
                this.grupo.getAdministrador() != null &&
                this.grupo.getAdministrador().getId().equals(this.id);
    }

    public boolean esPerfilIndependiente() {
        return this.rol == TipoRol.MIEMBRO || this.rol == TipoRol.EMPLEADO;
    }

    public boolean esAdministradorDePerfiles() {
        return this.esAdministrador() || this.esFamilia() || this.esEmpresa();
    }

    /**
     * Obtiene todos los perfiles a los que este usuario tiene acceso
     */
    public List<Perfil> getPerfilesAccesibles() {
        List<Perfil> accesibles = new ArrayList<>();

        // Si es administrador, tiene acceso a todos sus perfiles administrados
        if (this.esAdministradorDePerfiles()) {
            accesibles.addAll(this.perfilesAdministrados);
        }

        // Si es perfil independiente, solo tiene acceso a su perfil asociado
        if (this.esPerfilIndependiente() && this.perfilAsociado != null) {
            accesibles.add(this.perfilAsociado);
        }

        return accesibles;
    }

    /**
     * Obtiene el perfil personal del administrador
     */
    public Perfil getPerfilPersonal() {
        return this.perfilesAdministrados.stream()
                .filter(p -> p.getTipo() == TipoPerfil.PERSONAL && p.getActivo())
                .findFirst()
                .orElse(null);
    }

    /**
     * Obtiene los miembros de la familia (excluyendo personal)
     */
    public List<Perfil> getMiembrosFamilia() {
        return this.perfilesAdministrados.stream()
                .filter(p -> p.getTipo() == TipoPerfil.MIEMBRO && p.getActivo())
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los empleados (excluyendo personal)
     */
    public List<Perfil> getEmpleados() {
        return this.perfilesAdministrados.stream()
                .filter(p -> p.getTipo() == TipoPerfil.EMPLEADO && p.getActivo())
                .collect(Collectors.toList());
    }

    // ========== SPRING SECURITY ==========


    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.rol.name()));
    }


    public String getUsername() {
        return this.email;
    }


    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }


    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }


    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    public boolean isEnabled() {
        return enabled;
    }


}
