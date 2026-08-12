package Bumerak.administrador.dto.request;

import Bumerak.administrador.entidades.enums.TipoPerfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerfilRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String apellidos;

    @Email(message = "Email inválido")
    private String email;

    private String telefono;
    private String direccion;
    private String fotoPerfil;
    private LocalDateTime fechaNacimiento;

    @NotNull(message = "El tipo de perfil es obligatorio")
    private TipoPerfil tipo;
}

