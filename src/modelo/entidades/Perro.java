package modelo.entidades;

import java.time.LocalDate;

public class Perro extends Mascota {

    private final String numeroChip;
    private final double tamanio;

    public Perro(String nombre, String raza, String sexo,
                 LocalDate fechaNacimiento, double pesoActual,
                 Dueno dueno, String numeroChip, double tamanio) {

        super(nombre, raza, sexo, fechaNacimiento, pesoActual, dueno);

        if (numeroChip == null || numeroChip.isBlank()) {
            throw new IllegalArgumentException(
                    "El perro debe tener número de chip");
        }

        if (tamanio <= 0 || Double.isNaN(tamanio)
                || Double.isInfinite(tamanio)) {
            throw new IllegalArgumentException(
                    "El tamaño debe ser mayor a 0");
        }

        this.numeroChip = numeroChip.trim();
        this.tamanio = tamanio;
    }

    @Override
    public Especie getEspecie() {
        return Especie.PERRO;
    }

    public String getNumeroChip() {
        return numeroChip;
    }

    public double getTamanio() {
        return tamanio;
    }
}

