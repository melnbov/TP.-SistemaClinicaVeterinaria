package modelo.entidades;

public class Tratamiento {

    private final Medicamento medicamento;
    private final String dosis;
    private final String frecuencia;
    private final String duracion;

    Tratamiento(Medicamento medicamento, String dosis,
                String frecuencia, String duracion) {

        if (medicamento == null) {
            throw new IllegalArgumentException(
                    "El medicamento es obligatorio");
        }

        if (dosis == null || dosis.isBlank()) {
            throw new IllegalArgumentException(
                    "La dosis es obligatoria");
        }

        if (frecuencia == null || frecuencia.isBlank()) {
            throw new IllegalArgumentException(
                    "La frecuencia es obligatoria");
        }

        if (duracion == null || duracion.isBlank()) {
            throw new IllegalArgumentException(
                    "La duración es obligatoria");
        }

        this.medicamento = medicamento;
        this.dosis = dosis.trim();
        this.frecuencia = frecuencia.trim();
        this.duracion = duracion.trim();
    }

    public Medicamento getMedicamento() {
        return medicamento;
    }

    public String getDosis() {
        return dosis;
    }

    public String getFrecuencia() {
        return frecuencia;
    }

    public String getDuracion() {
        return duracion;
    }

    @Override
    public String toString() {
        return medicamento.getNombre()
                + " - Dosis: " + dosis
                + " - Frecuencia: " + frecuencia
                + " - Duración: " + duracion;
    }
}

