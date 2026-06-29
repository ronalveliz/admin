package Bumerak.administrador.entidades;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Movimiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double monto;
    private String tipo;
    private String descripcion;
    private LocalDate fecha;
    private Boolean esPeriodico;
    private String frecuencia;
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuarios usuario;

}
