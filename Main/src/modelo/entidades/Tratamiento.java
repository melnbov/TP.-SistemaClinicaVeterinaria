
package modelo.entidades;

public class Tratamiento {
    private final Medicamento medicamento;
    private final String dosis;
    private final String frecuencia;
    private final String duracion;

    public Tratamiento(Medicamento medicamento, String dosis, String frecuencia, String duracion) {
        if (medicamento == null){
            throw new IllegalArgumentException("El medicamento es obligatorio");
        }
        this.medicamento = medicamento;
        this.dosis = dosis;
        this.frecuencia = frecuencia;
        this.duracion = duracion;
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

}
