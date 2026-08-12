package Bumerak.administrador.dto.response;

import Bumerak.administrador.entidades.enums.TipoPerfil;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PerfilResumenResponse {
    private Long perfilId;
    private String perfilNombre;
    private TipoPerfil perfilTipo;
    private BigDecimal ingresos;
    private BigDecimal gastos;
    private BigDecimal balance;
    private Integer cantidadMovimientos;
}
