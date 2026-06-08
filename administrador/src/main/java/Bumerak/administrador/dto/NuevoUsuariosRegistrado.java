package Bumerak.administrador.dto;

import Bumerak.administrador.entidades.RolName;

public record NuevoUsuariosRegistrado(
        String email,
        String password,
        String nombre,
        String phone,
        RolName roleName,
        String imgUser
) {
}
