package entidades;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Gasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double monto;
    private String descripcion;
    private String fecha;

    @ManyToOne
    private Categoria categoria;

    @ManyToOne
    private Usuario usuario;
}
