package Bumerak.administrador.dto.response;
import Bumerak.administrador.entidades.Grupo;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GrupoResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private String tipo;  // FAMILIA o EMPRESA
    private Long administradorId;
    private String administradorNombre;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    /**
     * Convertir entidad Grupo a GrupoResponse
     */
    public static GrupoResponse fromEntity(Grupo grupo) {
        if (grupo == null) {
            return null;
        }

        return GrupoResponse.builder()
                .id(grupo.getId())
                .nombre(grupo.getNombre())
                .descripcion(grupo.getDescripcion())
                .tipo(grupo.getTipo() != null ? grupo.getTipo().name() : null)
                .administradorId(grupo.getAdministrador() != null ? grupo.getAdministrador().getId() : null)
                .administradorNombre(grupo.getAdministrador() != null ? grupo.getAdministrador().getNombre() : null)
                .activo(grupo.getActivo())
                .fechaCreacion(grupo.getFechaCreacion())
                .fechaActualizacion(grupo.getFechaActualizacion())
                .build();
    }
}