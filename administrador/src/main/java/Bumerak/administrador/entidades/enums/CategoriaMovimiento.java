package Bumerak.administrador.entidades.enums;

public enum CategoriaMovimiento {

    ALIMENTACION("Alimentación"),
    TRANSPORTE("Transporte"),
    VIVIENDA("Vivienda"),
    SERVICIOS("Servicios"),
    EDUCACION("Educación"),
    SALUD("Salud"),
    ENTRETENIMIENTO("Entretenimiento"),
    AHORRO("Ahorro"),
    INVERSION("Inversión"),
    SALARIO("Salario"),
    IMPUESTOS("Impuestos"),
    OTROS("Otros");

    private final String descripcion;

    CategoriaMovimiento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}

