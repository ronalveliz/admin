package controladores;

import dto.LoginUsuario;
import dto.NuevoUsuarioRegistrador;
import dto.Token;
import entidades.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import repositorios.UsuarioRepository;
import seguridad.jwt.JwtUtil;

import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@RestController
@AllArgsConstructor
@Slf4j
@CrossOrigin("*")
public class UsuarioControladores {

    private final UsuarioRepository UsuarioRepository;
    private final FileService fileService;
    private final PasswordEncoder passwordEncoder;


    @PostMapping("usuarios/register")
    public void register(@RequestBody NuevoUsuarioRegistrador register) {

        if (this.UsuarioRepository.existsByEmail(register.email())){
            throw new BadCredentialsException("Email ocupado. Elija otro email.");
        }

        Usuario user = Usuario.builder()
                .email(register.email())
                .password(passwordEncoder.encode(register.password()))
                .nombre(register.nombre())
                //.rolName(register.roleName()).rolName(RolName.USER)
                .imgUser(register.imgUser()).imgUser("https://www.pngkey.com/png/detail/230-2301779_best-classified-apps-default-user-profile.png")
                .build();
        this.UsuarioRepository.save(user);
    }

    @PostMapping("/usuarios/login")
    public Token login(@RequestBody LoginUsuario login) {
        JwtUtil.getCurrentUser().ifPresent(System.out::println);

        if (!UsuarioRepository.existsByEmail(login.email())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        Usuario user = UsuarioRepository.findByEmail(login.email()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if(!passwordEncoder.matches(login.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }
        Date issuedDate = new Date();
        long nextWeekMillis = TimeUnit.DAYS.toMillis(7);
        Date expirationDate = new Date(issuedDate.getTime() + nextWeekMillis);

        byte[] key = Base64.getDecoder().decode("4LyjLvUySomBqqvJPH3LZ6x9mwIuX12GdqhpYU1nrb4=");

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
                //.claim("rolname", user.getRolName())
                .claim("email", user.getEmail())
                // Construye el token
                .compact();
        return ResponseEntity.ok(new Token(token)).getBody();
    }

    //trae todos los usuarios
    @GetMapping("usuarios")
    public Usuario getCurrentUser() {
        return JwtUtil.getCurrentUser().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    @PostMapping("usuarios/avatar")
    public Usuario uploadAvatar(@RequestParam(value = "photo", required = false) MultipartFile file)
    {

        Usuario user = JwtUtil.getCurrentUser().orElseThrow();
        if (file != null){
            String fileName = fileService.usuario(file);
            user.setImgUser(fileName);
            this.UsuarioRepository.save(user);

        }
        return user;

    }

    @PutMapping("usuario/{id}")
    public ResponseEntity<Usuario> update(@RequestBody Usuario usuario, @PathVariable long id) {
        Usuario usuarioFromDB = UsuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (usuario.getId() != null && !usuario.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id del body no coincide con el id de la URL");
        }

        usuarioFromDB.setNombre(usuario.getNombre());

        return ResponseEntity.ok(UsuarioRepository.save(usuarioFromDB));
    }

    @DeleteMapping("usuario/{id}")
    private ResponseEntity<Void> deleteById(@PathVariable Long id){

        UsuarioRepository.deleteById(id);
        return ResponseEntity.noContent().build(); //204
    }

}
