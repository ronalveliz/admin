package Bumerak.administrador.repositorios;

import Bumerak.administrador.entidades.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {
    //lista de movimientos por usuario
    List<Movimiento> findByUsuarioId(Long usuarioId);

}