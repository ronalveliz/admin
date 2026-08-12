package Bumerak.administrador.entidades.enums;

public enum TipoRol {
    FAMILIA("Familia", "Usuario que administra una familia"),
    EMPRESA("Empresa", "Usuario que administra una empresa"),
    ADMINISTRADOR("Administrador", "Usuario con permisos totales"),
    MIEMBRO("Miembro", "Miembro de una familia con acceso independiente"),
    EMPLEADO("Empleado", "Empleado de una empresa con acceso independiente");

    private final String nombre;
    private final String descripcion;

    TipoRol(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean esAdministrador() {
        return this == ADMINISTRADOR || this == FAMILIA || this == EMPRESA;
    }

    public boolean esPerfilIndependiente() {
        return this == MIEMBRO || this == EMPLEADO;
    }
    /**
    * Obtiene el tipo de grupo asociado a este rol
     */
    public TipoGrupo getTipoGrupoAsociado() {
        return switch (this) {
            case FAMILIA -> TipoGrupo.FAMILIA;
            case EMPRESA -> TipoGrupo.EMPRESA;
            case ADMINISTRADOR, MIEMBRO, EMPLEADO -> null;
        };
    }
}

