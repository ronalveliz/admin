package Bumerak.administrador.dto.request;

import Bumerak.administrador.entidades.enums.TipoRol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegistroRequest {
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String telefono;
    private String direccion;
    private String imgUser;

    @NotNull(message = "El rol es obligatorio")
    private TipoRol roleName; // FAMILIA, EMPRESA, ADMINISTRADOR

    // Si es FAMILIA o EMPRESA
    private String nombreGrupo;
    private String descripcionGrupo;
}