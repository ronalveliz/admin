package Bumerak.administrador.entidades.enums;

public enum FrecuenciaMovimiento {
    DIARIO("Diario"),
    SEMANAL("Semanal"),
    MENSUAL("Mensual"),
    ANUAL("Anual"),
    UNICO("Único");

    private String descripcion;

    FrecuenciaMovimiento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
