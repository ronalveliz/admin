package seguridad.configuraciones;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import seguridad.jwt.JwtFilter;


@Configuration
@AllArgsConstructor

public class SegurityConfig {

   private final JwtFilter jwtFilter;

   @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

       http.csrf(AbstractHttpConfigurer::disable)
                 .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                 .authorizeHttpRequests(auth -> auth

                         .requestMatchers("usuarios/login", "usuarios/register","files/**", "users/avatar").permitAll()
                         //.requestMatchers(HttpMethod.GET).permitAll()
                         .requestMatchers("/usuarios/login").permitAll()
                         .requestMatchers(HttpMethod.POST, "/users/avatar").permitAll()
                         .requestMatchers(HttpMethod.PUT , "/users/avatar").permitAll()
                         .requestMatchers(HttpMethod.POST, "categoria", "ingreso", "gastos").hasAnyAuthority("ADMIN")
                         .requestMatchers(HttpMethod.PUT, "categoria", "ingreso", "gastos").hasAnyAuthority("ADMIN")
                         .requestMatchers(HttpMethod.DELETE, "categoria", "ingreso","gastos").hasAnyAuthority("ADMIN")

                         .anyRequest().authenticated())

                 .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

         return http.build();
     }

}
