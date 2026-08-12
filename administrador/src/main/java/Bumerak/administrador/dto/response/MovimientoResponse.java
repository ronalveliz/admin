package Bumerak.administrador.dto.response;

import Bumerak.administrador.entidades.Movimiento;
import Bumerak.administrador.entidades.enums.CategoriaMovimiento;
import Bumerak.administrador.entidades.enums.TipoMovimiento;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class MovimientoResponse {
    private Long id;
    private BigDecimal monto;
    private TipoMovimiento tipo;
    private CategoriaMovimiento categoria;
    private String descripcion;
    private LocalDate fecha;
    private String perfilNombre;
    private String usuarioNombre;

    public static MovimientoResponse fromEntity(Movimiento movimiento) {
        if (movimiento == null) {
            return null;
        }

        return MovimientoResponse.builder()
                .id(movimiento.getId())
                .monto(movimiento.getMonto())
                .tipo(movimiento.getTipo())
                .categoria(movimiento.getCategoria())
                .descripcion(movimiento.getDescripcion())
                .fecha(movimiento.getFecha())
                .perfilNombre(movimiento.getPerfil() != null ?
                        movimiento.getPerfil().getNombreCompleto() : null)
                .usuarioNombre(movimiento.getUsuario().getNombre())
                .build();
    }
}