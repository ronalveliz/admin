package Bumerak.administrador.entidades.enums;

public enum TipoMovimiento {
    INGRESO("Ingreso"),
    GASTO("Gasto"),
    TRANSFERENCIA("Transferencia");

    private final String descripcion;

    TipoMovimiento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}

