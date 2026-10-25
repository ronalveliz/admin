package Bumerak.administrador.seguridad.configuraciones;

import Bumerak.administrador.seguridad.jwt.JwtFilter;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // ✅ Necesario para @PreAuthorize
@AllArgsConstructor
public class SegurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        config.addExposedHeader("Authorization");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Rutas PÚBLICAS
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/auth/login",
                                "/api/auth/registro",
                                "/files/**",
                                "/h2-console/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ✅ PERFILES - Cualquier usuario autenticado puede ver
                        .requestMatchers(HttpMethod.GET, "/api/perfiles/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/perfiles/**")
                        .hasAnyAuthority("ROLE_ADMINISTRADOR", "ROLE_FAMILIA", "ROLE_EMPRESA")
                        .requestMatchers(HttpMethod.PUT, "/api/perfiles/**")
                        .hasAnyAuthority("ROLE_ADMINISTRADOR", "ROLE_FAMILIA", "ROLE_EMPRESA")
                        .requestMatchers(HttpMethod.DELETE, "/api/perfiles/**")
                        .hasAnyAuthority("ROLE_ADMINISTRADOR", "ROLE_FAMILIA", "ROLE_EMPRESA")

                        // ✅ MOVIMIENTOS - Cualquier usuario autenticado
                        .requestMatchers("/api/movimientos/**").authenticated()

                        // ✅ USUARIOS - Solo administradores
                        .requestMatchers("/api/usuarios/**").hasAuthority("ROLE_ADMINISTRADOR")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}