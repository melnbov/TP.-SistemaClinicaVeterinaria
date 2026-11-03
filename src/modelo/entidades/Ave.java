package modelo.entidades;

import java.time.LocalDate;

public class Ave extends Mascota{

    private final boolean vuelo;

    public Ave(String nombre, String raza, String sexo,
               LocalDate fechaNacimiento, double pesoActual,
               Dueno dueno, boolean vuelo) {

        super(nombre, raza, sexo, fechaNacimiento, pesoActual, dueno);

        this.vuelo = vuelo;
    }

    @Override
    public Especie getEspecie(){
        return Especie.AVE;
    }

    public boolean puedeVolar() {
        return vuelo;
    }
}

