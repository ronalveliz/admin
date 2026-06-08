package Bumerak.administrador.controladores;

import Bumerak.administrador.dto.LoginUsuarios;
import Bumerak.administrador.dto.NuevoUsuariosRegistrado;
import Bumerak.administrador.dto.Token;
import Bumerak.administrador.entidades.RolName;
import Bumerak.administrador.entidades.Usuarios;
import Bumerak.administrador.exception.FileException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import Bumerak.administrador.repositorios.UsuariosRepository;
import Bumerak.administrador.seguridad.jwt.JwtUtil;
import Bumerak.administrador.servicios.FileService;

import java.util.*;
import java.util.concurrent.TimeUnit;

@CrossOrigin("*")
@Slf4j
@RestController
@AllArgsConstructor
public class UsuariosController {

    private final UsuariosRepository repo;
    private final FileService fileService;
    private final PasswordEncoder passwordEncoder;


    @GetMapping("users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Usuarios> findAll(){

        return repo.findAll();
    }

    @PostMapping("users/register")
    public void register(@RequestBody NuevoUsuariosRegistrado register) {

        if (this.repo.existsByEmail(register.email())){
            throw new BadCredentialsException("Email ocupado. Elija otro email.");
        }

        Usuarios user = Usuarios.builder()
                .email(register.email())
                .password(passwordEncoder.encode(register.password()))
                .nombre(register.nombre())
                .rolName(register.roleName()).rolName(RolName.USUARIOS)
                .imgUser(register.imgUser()).imgUser("https://www.pngkey.com/png/detail/230-2301779_best-classified-apps-default-user-profile.png")
                .build();
        this.repo.save(user);
    }

    @PostMapping("/users/login")
    public Token login(@RequestBody LoginUsuarios login) {
        JwtUtil.getCurrentUser().ifPresent(System.out::println);

        if (!repo.existsByEmail(login.email())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        Usuarios user = repo.findByEmail(login.email()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if(!passwordEncoder.matches(login.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }
        Date issuedDate = new Date();
        long nextWeekMillis = TimeUnit.DAYS.toMillis(7);
        Date expirationDate = new Date(issuedDate.getTime() + nextWeekMillis);

        byte[] key = Base64.getDecoder().decode("4PWbGp0oV5si8hXJS0Hl/yk9RWX7SZK7DdckNx3e0cQ=");

        String token = Jwts.builder()
                // id del usuario
                .subject(String.valueOf(user.getId()))
                // La clave secreta para firmar el token y saber que es nuestro cuando lleguen las peticiones del frontend
                .signWith(Keys.hmacShaKeyFor(key))
                // Fecha emisión del token
                .issuedAt(issuedDate)
                // Fecha de expiración del token
                .expiration(expirationDate)
                // información personalizada: rol, username, email...
                .claim("rolname", user.getRolName())
                .claim("email", user.getEmail())
                // Construye el token
                .compact();
        return ResponseEntity.ok(new Token(token)).getBody();


    }
    // Get account
    @GetMapping("users/account")
    @PreAuthorize("hasRole('ADMIN')")
    public Usuarios getCurrentUser() {
        return JwtUtil.getCurrentUser().orElseThrow();
    }


    @PutMapping("user/account")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TIENDA')")
    public Usuarios update(@RequestBody Usuarios user) {
        // Si está autenticado, y el usuario autenticado es ADMIN o es el mismo usuario que la variable user
        // entonces actualizar, en caso contrario no actualizamos
        JwtUtil.getCurrentUser().ifPresent(currentUser -> {
            if (currentUser.getRolName() == RolName.ADMINISTRADOR|| Objects.equals(currentUser.getId(), user.getId())) {
                this.repo.save(user);
            } else {
                throw new RuntimeException("No puede actualizar"); // Reemplazar por Excepción personalizada
            }
        });

        return user;
    }

    // subir avatar
    @PostMapping("user/account/avatar")
    @PreAuthorize("hasRole('ADMIN')")
    public Usuarios uploadAvatar(
            @RequestParam(value = "photo") MultipartFile file
    ) throws FileException {

        Usuarios user = JwtUtil.getCurrentUser().orElseThrow();

        if (file != null && !file.isEmpty()) {
            String fileName = fileService.store(file);
            user.setImgUser(fileName);
            this.repo.save(user);
        }

        return user;
    }
    // subir avatar
    @PutMapping ("user/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    private ResponseEntity<Usuarios> update(@RequestBody Usuarios user, @PathVariable Long id){
        Optional<Usuarios> userOtp = repo.findById(id);
        if (user.getId() == null){
            return ResponseEntity.badRequest().build();
        }
        Usuarios usuariosFromDB = userOtp.get();
        // faltan mas atributos
        return ResponseEntity.ok(repo.save(usuariosFromDB));
    }


    @DeleteMapping("user/id")
    @PreAuthorize("hasRole('ADMIN')")
    private ResponseEntity<Void> deleteById(@PathVariable Long id){
        repo.deleteById(id);
        return ResponseEntity.noContent().build(); //204
    }
}

