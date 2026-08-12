package Bumerak.administrador.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;


@Data
@Builder
public class ResumenMensualResponse {

    private YearMonth mes;
    private BigDecimal totalIngresos;
    private BigDecimal totalGastos;
    private BigDecimal balance;
    private Map<String, BigDecimal> gastosPorCategoria;
    private List<PerfilResumenResponse> resumenPorPerfil;  // ✅ Usa la clase independiente
    private List<MovimientoResponse> movimientosRecientes;
}
