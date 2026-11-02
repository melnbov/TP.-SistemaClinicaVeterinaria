package clinica.modelo.entidades;

import java.time.LocalDate;

public class Gato extends Mascota {

    private final EstiloVida vida;

    public Gato(String nombre, String raza, String sexo,
                LocalDate fechaNacimiento, double pesoActual,
                Dueno dueno, EstiloVida vida) {

        super(nombre, raza, sexo, fechaNacimiento, pesoActual, dueno);

        if (vida == null) {
            throw new IllegalArgumentException(
                    "Debe indicarse si el gato vive en interior o exterior");
        }

        this.vida = vida;
    }

    @Override
    public Especie getEspecie() {
        return Especie.GATO;
    }

    public EstiloVida getVida() {
        return vida;
    }
}

