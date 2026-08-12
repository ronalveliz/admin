package Bumerak.administrador.seguridad.jwt;

import Bumerak.administrador.entidades.Usuarios;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.IOException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import Bumerak.administrador.repositorios.UsuariosRepository;

import java.util.Base64;
import java.util.List;
import java.util.Optional;


@Component
@AllArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    private final UsuariosRepository userRepository;
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException, java.io.IOException {

        log.info("🔍 Procesando petición: {}", request.getRequestURI());

        // Extraer token de la cabecera Authorization
        String bearerToken = request.getHeader("Authorization");

        if (!StringUtils.hasLength(bearerToken) || !bearerToken.startsWith("Bearer ")) {
            log.debug("⚠️ No se encontró token Bearer");
            filterChain.doFilter(request, response);
            return;
        }

        String token = bearerToken.substring("Bearer ".length());
        log.debug("📝 Token extraído");

        try {
            // Validar el token usando JwtUtil
            if (!jwtUtil.validateToken(token)) {
                log.warn("❌ Token inválido");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido");
                return;
            }

            // Extraer ID del usuario del token
            Long usuarioId = jwtUtil.getUsuarioIdFromToken(token);
            String rol = jwtUtil.getRolFromToken(token);

            log.info("👤 Usuario ID: {} - Rol: {}", usuarioId, rol);

            // Obtener el usuario de la base de datos
            Optional<Usuarios> userOptional = userRepository.findById(usuarioId);

            if (userOptional.isEmpty()) {
                log.warn("❌ Usuario no encontrado con ID: {}", usuarioId);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Usuario no encontrado");
                return;
            }

            Usuarios user = userOptional.get();

            // Verificar que el usuario esté activo
            if (!user.getEnabled()) {
                log.warn("❌ Usuario deshabilitado: {}", user.getEmail());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Usuario deshabilitado");
                return;
            }

            // Crear objeto de autenticación
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + rol);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, List.of(authority));

            SecurityContextHolder.getContext().setAuthentication(auth);
            log.info("✅ Usuario autenticado: {}", user.getEmail());

        } catch (Exception e) {
            log.error("❌ Error al procesar token: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Error de autenticación");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Excluir rutas públicas del filtro
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/registro") ||
                path.startsWith("/users/login") ||
                path.startsWith("/users/register") ||
                path.startsWith("/files/") ||
                path.startsWith("/h2-console");
    }
}