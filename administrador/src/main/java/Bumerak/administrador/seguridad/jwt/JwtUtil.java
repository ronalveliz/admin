package Bumerak.administrador.seguridad.jwt;

import Bumerak.administrador.entidades.Usuarios;
import org.springframework.security.core.context.SecurityContextHolder;


import java.util.Optional;


public class JwtUtil {

    public static Optional<Usuarios> getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof Usuarios user) {
            return Optional.of(user);
        } else {
            return Optional.empty();
        }
    }

}
