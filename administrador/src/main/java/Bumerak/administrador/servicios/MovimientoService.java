package Bumerak.administrador.servicios;

import Bumerak.administrador.entidades.Movimiento;
import Bumerak.administrador.repositorios.MovimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovimientoService {
    @Autowired
    private MovimientoRepository repository;

    public Movimiento guardar(Movimiento movimiento) {
        return repository.save(movimiento);
    }

    public List<Movimiento>listarPorUsuario(Long idUsuario) {
        return repository.findByUsuarioId(idUsuario);
    }
}




