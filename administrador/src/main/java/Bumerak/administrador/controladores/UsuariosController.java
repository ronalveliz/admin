package Bumerak.administrador.controladores;

import Bumerak.administrador.dto.response.PerfilResponse;
import Bumerak.administrador.entidades.Perfil;
import Bumerak.administrador.entidades.enums.TipoRol;
import Bumerak.administrador.entidades.Usuarios;
import Bumerak.administrador.exception.CustomException;
import Bumerak.administrador.exception.FileException;
import Bumerak.administrador.servicios.PerfilService;
import Bumerak.administrador.servicios.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import Bumerak.administrador.repositorios.UsuariosRepository;
import Bumerak.administrador.seguridad.jwt.JwtUtil;
import Bumerak.administrador.servicios.FileService;

import java.util.*;
import java.util.stream.Collectors;

    @CrossOrigin("*")
    @Slf4j
    @RestController
    @RequiredArgsConstructor
    @RequestMapping("/api/usuarios")
    public class UsuariosController {

        private final UsuariosRepository usuariosRepository;
        private final UsuarioService usuarioService;
        private final PerfilService perfilService;
        private final FileService fileService;
        private final PasswordEncoder passwordEncoder;

        // ========== OBTENER TODOS LOS USUARIOS ==========
        @GetMapping
        @PreAuthorize("hasRole('ADMINISTRADOR')")
        public ResponseEntity<List<Usuarios>> findAll() {
            log.info("📋 Obteniendo todos los usuarios");
            List<Usuarios> usuarios = usuariosRepository.findAll();
            return ResponseEntity.ok(usuarios);
        }

        // ========== OBTENER USUARIO POR ID ==========
        @GetMapping("/{id}")
        @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('FAMILIA') or hasRole('EMPRESA')")
        public ResponseEntity<Usuarios> findById(@PathVariable Long id) {
            log.info("📋 Obteniendo usuario ID: {}", id);
            Usuarios usuario = usuarioService.getUsuarioById(id);
            return ResponseEntity.ok(usuario);
        }

        // ========== OBTENER USUARIO ACTUAL ==========
        @GetMapping("/account")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<Usuarios> getCurrentUser() {
            log.info("📋 Obteniendo usuario actual");
            Usuarios usuario = JwtUtil.getCurrentUser()
                    .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));
            return ResponseEntity.ok(usuario);
        }

        // ========== OBTENER PERFILES DEL USUARIO ACTUAL ==========
        @GetMapping("/account/perfiles")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<List<PerfilResponse>> getCurrentUserPerfiles() {
            log.info("📋 Obteniendo perfiles del usuario actual");

            Usuarios usuario = JwtUtil.getCurrentUser()
                    .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

            List<Perfil> perfiles = usuario.getPerfilesAccesibles();
            List<PerfilResponse> response = perfiles.stream()
                    .map(PerfilResponse::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        }

        // ========== ACTUALIZAR USUARIO ==========
        @PutMapping("/account")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<Usuarios> updateCurrentUser(@RequestBody Usuarios user) {
            log.info("✏️ Actualizando usuario actual");

            Usuarios currentUser = JwtUtil.getCurrentUser()
                    .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

            // Solo ADMIN o el mismo usuario pueden actualizar
            if (currentUser.getRol() != TipoRol.ADMINISTRADOR &&
                    !Objects.equals(currentUser.getId(), user.getId())) {
                throw new CustomException("No tienes permiso para actualizar este usuario", HttpStatus.FORBIDDEN);
            }

            // Actualizar solo campos permitidos
            currentUser.setNombre(user.getNombre());
            currentUser.setTelefono(user.getTelefono());
            currentUser.setDireccion(user.getDireccion());

            // Si es ADMIN, puede cambiar el rol
            if (currentUser.getRol() == TipoRol.ADMINISTRADOR && user.getRol() != null) {
                currentUser.setRol(user.getRol());
            }

            Usuarios updatedUser = usuariosRepository.save(currentUser);
            log.info("✅ Usuario actualizado: {}", updatedUser.getEmail());

            return ResponseEntity.ok(updatedUser);
        }

        // ========== CAMBIAR CONTRASEÑA ==========
        @PutMapping("/account/password")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<Void> changePassword(
                @RequestParam String currentPassword,
                @RequestParam String newPassword) {

            log.info("🔑 Cambiando contraseña");

            Usuarios currentUser = JwtUtil.getCurrentUser()
                    .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

            // Verificar contraseña actual
            if (!passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
                throw new CustomException("Contraseña actual incorrecta", HttpStatus.UNAUTHORIZED);
            }

            // Actualizar contraseña
            currentUser.setPassword(passwordEncoder.encode(newPassword));
            usuariosRepository.save(currentUser);

            log.info("✅ Contraseña actualizada para: {}", currentUser.getEmail());
            return ResponseEntity.ok().build();
        }

        // ========== SUBIR AVATAR ==========
        @PostMapping("/account/avatar")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<Usuarios> uploadAvatar(@RequestParam("photo") MultipartFile file) {
            log.info("📤 Subiendo avatar");

            Usuarios currentUser = JwtUtil.getCurrentUser()
                    .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

            try {
                if (file != null && !file.isEmpty()) {
                    String fileName = fileService.store(file);
                    currentUser.setFotoPerfil(fileName);
                    usuariosRepository.save(currentUser);
                    log.info("✅ Avatar actualizado para: {}", currentUser.getEmail());
                }
                return ResponseEntity.ok(currentUser);
            } catch (FileException e) {
                log.error("❌ Error al subir avatar: {}", e.getMessage());
                throw new CustomException("Error al subir avatar: " + e.getMessage(), HttpStatus.BAD_REQUEST);
            }
        }

        // ========== ACTUALIZAR USUARIO POR ID (SOLO ADMIN) ==========
        @PutMapping("/{id}")
        @PreAuthorize("hasRole('ADMINISTRADOR')")
        public ResponseEntity<Usuarios> updateUserById(
                @PathVariable Long id,
                @RequestBody Usuarios user) {

            log.info("✏️ Actualizando usuario ID: {}", id);

            Usuarios existingUser = usuarioService.getUsuarioById(id);

            // Actualizar campos
            existingUser.setNombre(user.getNombre());
            existingUser.setTelefono(user.getTelefono());
            existingUser.setDireccion(user.getDireccion());
            existingUser.setFotoPerfil(user.getFotoPerfil());

            if (user.getRol() != null) {
                existingUser.setRol(user.getRol());
            }

            if (user.getEnabled() != null) {
                existingUser.setEnabled(user.getEnabled());
            }

            Usuarios updatedUser = usuariosRepository.save(existingUser);
            log.info("✅ Usuario actualizado por ADMIN: {}", updatedUser.getEmail());

            return ResponseEntity.ok(updatedUser);
        }

        // ========== ELIMINAR USUARIO (SOLO ADMIN) ==========
        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMINISTRADOR')")
        public ResponseEntity<Void> deleteById(@PathVariable Long id) {
            log.info("🗑️ Eliminando usuario ID: {}", id);

            Usuarios usuario = usuarioService.getUsuarioById(id);

            // No permitir eliminar al propio admin
            Usuarios currentUser = JwtUtil.getCurrentUser()
                    .orElseThrow(() -> new CustomException("Usuario no autenticado", HttpStatus.UNAUTHORIZED));

            if (Objects.equals(currentUser.getId(), id)) {
                throw new CustomException("No puedes eliminar tu propio usuario", HttpStatus.BAD_REQUEST);
            }

            // Deshabilitar lógicamente en lugar de eliminar
            usuario.setEnabled(false);
            usuariosRepository.save(usuario);

            log.info("✅ Usuario deshabilitado: {}", usuario.getEmail());
            return ResponseEntity.noContent().build();
        }

        // ========== HABILITAR USUARIO (SOLO ADMIN) ==========
        @PutMapping("/{id}/habilitar")
        @PreAuthorize("hasRole('ADMINISTRADOR')")
        public ResponseEntity<Void> habilitarUsuario(@PathVariable Long id) {
            log.info("🔄 Habilitando usuario ID: {}", id);

            Usuarios usuario = usuarioService.getUsuarioById(id);
            usuario.setEnabled(true);
            usuariosRepository.save(usuario);

            log.info("✅ Usuario habilitado: {}", usuario.getEmail());
            return ResponseEntity.ok().build();
        }

        // ========== OBTENER USUARIOS POR ROL ==========
        @GetMapping("/rol/{rol}")
        @PreAuthorize("hasRole('ADMINISTRADOR')")
        public ResponseEntity<List<Usuarios>> findByRol(@PathVariable TipoRol rol) {
            log.info("📋 Obteniendo usuarios con rol: {}", rol);
            List<Usuarios> usuarios = usuariosRepository.findByRol(rol);
            return ResponseEntity.ok(usuarios);
        }
    }
