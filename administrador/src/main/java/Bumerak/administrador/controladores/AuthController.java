package Bumerak.administrador.controladores;

import Bumerak.administrador.dto.LoginUsuarios;
import Bumerak.administrador.dto.NuevoUsuariosRegistrado;
import Bumerak.administrador.dto.response.AuthResponse;
import Bumerak.administrador.exception.CustomException;
import Bumerak.administrador.servicios.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;

    /**
     * Registro de nuevo usuario
     * POST /api/auth/registro
     */
    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody NuevoUsuariosRegistrado request) {
        log.info("📝 Solicitud de registro para: {}", request.email());
        AuthResponse response = authService.registrar(request);
        log.info("✅ Registro exitoso para: {}", request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login de usuario
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginUsuarios request) throws CustomException {
        log.info("📝 Solicitud de login para: {}", request.email());
        AuthResponse response = authService.autenticar(request);
        log.info("✅ Login exitoso para: {}", request.email());
        return ResponseEntity.ok(response);
    }

    /**
     * Login con perfil específico
     * POST /api/auth/login/perfil/{perfilId}
     */
    @PostMapping("/login/perfil/{perfilId}")
    public ResponseEntity<AuthResponse> loginConPerfil(
            @Valid @RequestBody LoginUsuarios request,
            @PathVariable Long perfilId) {
        log.info("📝 Solicitud de login para: {} con perfil ID: {}", request.email(), perfilId);
        AuthResponse response = authService.autenticarConPerfil(request, perfilId);
        log.info("✅ Login exitoso para: {} con perfil ID: {}", request.email(), perfilId);
        return ResponseEntity.ok(response);
    }
}
