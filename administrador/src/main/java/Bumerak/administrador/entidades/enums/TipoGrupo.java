package Bumerak.administrador.entidades.enums;

public enum TipoGrupo {
    FAMILIA("Familia", "Grupo familiar"),
    EMPRESA("Empresa", "Grupo empresarial");

    private final String nombre;
    private final String descripcion;

    TipoGrupo(String nombre, String descripcion) {
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

