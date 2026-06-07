package seguridad.jwt;


import entidades.Usuario;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
public class JwtUtil {

    public static Optional<Usuario>getCurrentUser(){
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof Usuario user) {
            return Optional.of(user);
        } else {
            return Optional.empty();
        }
    }

}
