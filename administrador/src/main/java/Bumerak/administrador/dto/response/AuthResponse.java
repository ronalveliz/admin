package Bumerak.administrador.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AuthResponse {

    private String token;
    private String tipo;
    private Long expiracion;

    private UsuarioInfo usuario;
    private PerfilResponse perfilActivo;
    private List<PerfilResponse> todosLosPerfiles;
    private GrupoInfo grupo;
    private List<String> permisos;

    @Data
    @Builder
    public static class UsuarioInfo {
        private Long id;
        private String email;
        private String nombre;
        private String rol;
        private String telefono;
        private String direccion;
        private String fotoPerfil;
    }

    @Data
    @Builder
    public static class GrupoInfo {
        private Long id;
        private String nombre;
        private String tipo;
        private String descripcion;
    }
}

