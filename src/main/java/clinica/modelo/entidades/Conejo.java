package clinica.modelo.entidades;

import java.time.LocalDate;

public class Conejo extends Mascota {

    public Conejo(String nombre, String raza, String sexo,
                  LocalDate fechaNacimiento, double pesoActual, Dueno dueno) {
        super(nombre, raza, sexo, fechaNacimiento, pesoActual, dueno);
    }

    @Override
    public Especie getEspecie() {
        return Especie.CONEJO;
    }
}


