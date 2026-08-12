package Bumerak.administrador.dto.request;

import Bumerak.administrador.entidades.enums.CategoriaMovimiento;
import Bumerak.administrador.entidades.enums.TipoMovimiento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MovimientoRequest {

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    @NotNull(message = "El tipo es obligatorio")
    private TipoMovimiento tipo;

    @NotNull(message = "La categoría es obligatoria")
    private CategoriaMovimiento categoria;

    private String descripcion;
    private LocalDate fecha;

    private Long perfilId; // Opcional: si se quiere asignar a un perfil específico
}