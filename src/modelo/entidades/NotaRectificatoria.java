package modelo.entidades;

import java.time.LocalDate;

public class NotaRectificatoria implements java.io.Serializable {

    private final LocalDate fecha;
    private final String texto;
    private final Veterinario firmante;

    NotaRectificatoria(LocalDate fecha, String texto,
                              Veterinario firmante) {

        if (fecha == null || fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La fecha de la nota no es válida");
        }

        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(
                    "El texto de la nota no puede estar vacío");
        }

        if (firmante == null) {
            throw new IllegalArgumentException(
                    "La nota debe estar firmada por un veterinario");
        }

        this.fecha = fecha;
        this.texto = texto.trim();
        this.firmante = firmante;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getTexto() {
        return texto;
    }

    public Veterinario getFirmante() {
        return firmante;
    }

    @Override
    public String toString() {
        return "Nota rectificatoria (" + fecha + "): "
                + texto + " - Firmada por: " + firmante;
    }
}

