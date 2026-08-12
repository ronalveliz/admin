package Bumerak.administrador.dto;

import Bumerak.administrador.entidades.enums.TipoRol;

public record NuevoUsuariosRegistrado(
        String email,
        String password,
        String nombre,
        String telefono,
        TipoRol roleName,
        String imgUser,
        String nombreGrupo,      // Nuevo
        String descripcionGrupo
) {
}
