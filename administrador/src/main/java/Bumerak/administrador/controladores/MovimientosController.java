package Bumerak.administrador.controladores;

import Bumerak.administrador.entidades.Movimiento;
import Bumerak.administrador.servicios.MovimientoService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@Slf4j
@RestController
@AllArgsConstructor
public class MovimientosController {
    @Autowired
    private MovimientoService service;

    @PostMapping
    public ResponseEntity<Movimiento> crear (@RequestBody Movimiento movimiento){
        return ResponseEntity.ok(service.guardar(movimiento));
    }
    @GetMapping("/{userId}")
    public ResponseEntity<List<Movimiento>> listar(@PathVariable Long userId) {
        return ResponseEntity.ok(service.listarPorUsuario(userId));
    }
    
}
