package Bumerak.administrador.dto.response;

import Bumerak.administrador.entidades.Perfil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerfilResponse {
    private Long id;
    private String nombre;
    private String apellidos;
    private String nombreCompleto;
    private String email;
    private String telefono;
    private String direccion;
    private String fotoPerfil;
    private String tipo; // PERSONAL, MIEMBRO, EMPLEADO
    private BigDecimal totalIngresos;
    private BigDecimal totalGastos;
    private BigDecimal balance;
    private Boolean tieneAccesoIndependiente;
    private Boolean activo;

    /**
     * Convertir entidad Perfil a PerfilResponse
     */
    public static PerfilResponse fromEntity(Perfil perfil) {
        if (perfil == null) {
            return null;
        }

        return PerfilResponse.builder()
                .id(perfil.getId())
                .nombre(perfil.getNombre())
                .apellidos(perfil.getApellidos())
                .nombreCompleto(perfil.getNombreCompleto())
                .email(perfil.getEmail())
                .telefono(perfil.getTelefono())
                .direccion(perfil.getDireccion())
                .fotoPerfil(perfil.getFotoPerfil())
                .tipo(perfil.getTipo() != null ? perfil.getTipo().name() : null)
                .totalIngresos(perfil.getTotalIngresos() != null ? perfil.getTotalIngresos() : BigDecimal.ZERO)
                .totalGastos(perfil.getTotalGastos() != null ? perfil.getTotalGastos() : BigDecimal.ZERO)
                .balance(perfil.getBalance() != null ? perfil.getBalance() : BigDecimal.ZERO)
                .tieneAccesoIndependiente(perfil.getTieneAccesoIndependiente())
                .activo(perfil.getActivo())
                .build();
    }

    /**
     * Convertir lista de Perfil a lista de PerfilResponse
     */
    public static List<PerfilResponse> fromEntityList(List<Perfil> perfiles) {
        if (perfiles == null) {
            return new ArrayList<>();
        }
        return perfiles.stream()
                .map(PerfilResponse::fromEntity)
                .collect(Collectors.toList());
    }
}

