package Bumerak.administrador.seguridad.configuraciones;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import Bumerak.administrador.seguridad.jwt.JwtFilter;

import java.util.List;


@Configuration
@AllArgsConstructor

public class SegurityConfig {

    public final JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Configuración CORS
        http.cors(cors -> cors.configurationSource(request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(List.of("http://localhost:4200"));
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));
            config.setAllowCredentials(true);
            config.setMaxAge(3600L);
            return config;
        }));

        // Configuración CSRF y manejo de sesión
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Configuración de autorización
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/users/login",
                        "/users/register",
                        "/files/**",
                        "/users/account/avatar"
                ).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/productos/**", "/category/**","/users/**").permitAll()
                .requestMatchers(
                        HttpMethod.POST,
                        "/users/**",
                        "/productos/**",
                        "/category/**",
                        "/tienda/**"
                ).hasAnyAuthority("ROLE_ADMIN", "ROLE_TIENDA")
                .requestMatchers(
                        HttpMethod.PUT,
                        "/users/**",
                        "/productos/**",
                        "/category/**",
                        "/tienda/**"
                ).hasAnyAuthority("ROLE_ADMIN", "ROLE_TIENDA")
                .requestMatchers(
                        HttpMethod.DELETE,
                        "/users/**",
                        "/productos/**",
                        "/category/**"
                ).hasAuthority("ROLE_ADMIN")
                .anyRequest().authenticated()
        );

        // Añadir filtro JWT
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}