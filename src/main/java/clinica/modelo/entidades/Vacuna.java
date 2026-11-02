package clinica.modelo.entidades;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class Vacuna {

    private final String codigo;
    private final String nombre;
    private final Set<Especie> especies;
    private final int edadMinimaSemanas;
    private final int intervaloRefuerzoDias;

    public Vacuna(String codigo, String nombre, Set<Especie> especies,
                  int edadMinimaSemanas, int intervaloRefuerzoDias) {

        if (codigo == null || codigo.isBlank()
                || nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El código y el nombre de la vacuna son obligatorios");
        }

        if (especies == null || especies.isEmpty()) {
            throw new IllegalArgumentException(
                    "La vacuna debe aplicarse al menos a una especie");
        }

        for (Especie especie : especies) {
            if (especie == null) {
                throw new IllegalArgumentException(
                        "La lista de especies no puede contener valores nulos");
            }
        }

        if (edadMinimaSemanas < 0 || intervaloRefuerzoDias <= 0) {
            throw new IllegalArgumentException(
                    "La edad mínima o el intervalo de refuerzo son inválidos");
        }

        this.codigo = codigo.trim();
        this.nombre = nombre.trim();
        this.especies = Collections.unmodifiableSet(
                EnumSet.copyOf(especies));
        this.edadMinimaSemanas = edadMinimaSemanas;
        this.intervaloRefuerzoDias = intervaloRefuerzoDias;
    }

    public boolean aplicaA(Especie especie) {
        return especie != null && especies.contains(especie);
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public Set<Especie> getEspecies() {
        return especies;
    }

    public int getEdadMinimaSemanas() {
        return edadMinimaSemanas;
    }

    public int getIntervaloRefuerzoDias() {
        return intervaloRefuerzoDias;
    }

    @Override
    public String toString() {
        return nombre + " (" + codigo + ")";
    }
}

