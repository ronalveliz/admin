package Bumerak.administrador.entidades.enums;

public enum TipoPerfil {
    PERSONAL("Personal", "Perfil personal del administrador"),
    MIEMBRO("Miembro", "Miembro de la familia"),
    EMPLEADO("Empleado", "Empleado de la empresa");

    private final String nombre;
    private final String descripcion;

    TipoPerfil(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }
}