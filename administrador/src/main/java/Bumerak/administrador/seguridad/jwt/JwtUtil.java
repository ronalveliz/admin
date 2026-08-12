package Bumerak.administrador.seguridad.jwt;

import Bumerak.administrador.entidades.Perfil;
import Bumerak.administrador.entidades.Usuarios;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


import javax.crypto.SecretKey;
import java.security.Key;
import java.util.*;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * Obtener la clave secreta
     */
    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generar token JWT con información del usuario y perfil activo
     */
    public String generarTokenConPerfil(Usuarios usuario, Perfil perfilActivo) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", usuario.getRol().name());
        claims.put("perfilId", perfilActivo.getId());
        claims.put("perfilNombre", perfilActivo.getNombreCompleto());
        claims.put("perfilTipo", perfilActivo.getTipo().name());
        claims.put("grupoId", usuario.getGrupo() != null ? usuario.getGrupo().getId() : null);
        claims.put("esAdminGrupo", usuario.esAdminDeGrupo());
        claims.put("esAdminPerfiles", usuario.esAdministradorDePerfiles());

        return Jwts.builder()
                .claims(claims)                                           // ✅ Nuevo
                .subject(usuario.getId().toString())                      // ✅ Nuevo
                .issuedAt(new Date())                                     // ✅ Nuevo
                .expiration(new Date(System.currentTimeMillis() + expiration * 1000)) // ✅ Nuevo
                .signWith(getSecretKey())                                 // ✅ Nuevo (sin SignatureAlgorithm)
                .compact();
    }

    /**
     * Obtener el ID del perfil activo desde el token
     */
    public Long getPerfilIdFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims.get("perfilId", Long.class);
    }

    /**
     * Obtener el ID del usuario desde el token
     */
    public Long getUsuarioIdFromToken(String token) {
        Claims claims = extractClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    /**
     * Obtener el rol del usuario desde el token
     */
    public String getRolFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims.get("rol", String.class);
    }

    /**
     * Verificar si el token es válido
     */
    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Token inválido: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extraer claims del token - VERSIÓN 0.12.x
     */
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())      // ✅ Nuevo: verifyWith
                .build()
                .parseSignedClaims(token)        // ✅ Nuevo: parseSignedClaims
                .getPayload();                   // ✅ Nuevo: getPayload
    }

    /**
     * Obtener el usuario autenticado actual
     */
    public static Optional<Usuarios> getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof Usuarios user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /**
     * Obtener tiempo de expiración en segundos
     */
    public Long getExpirationTime() {
        return expiration;
    }
}