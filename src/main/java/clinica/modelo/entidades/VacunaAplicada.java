package clinica.modelo.entidades;

import java.time.LocalDate;

public class VacunaAplicada {

    private static final int DIAS_AVISO_PREVIO = 7;
    private static final int DIAS_PARA_VENCER = 30;

    private final Vacuna vacuna;
    private final String lote;
    private final LocalDate fechaAplicacion;

    VacunaAplicada(Vacuna vacuna, String lote, LocalDate fechaAplicacion) {
        if (vacuna == null) {
            throw new IllegalArgumentException("La vacuna es obligatoria");
        }

        if (lote == null || lote.isBlank()) {
            throw new IllegalArgumentException("El lote es obligatorio");
        }

        if (fechaAplicacion == null
                || fechaAplicacion.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La fecha de aplicación no es válida");
        }

        this.vacuna = vacuna;
        this.lote = lote.trim();
        this.fechaAplicacion = fechaAplicacion;
    }

    public LocalDate calcularProximoRefuerzo() {
        return fechaAplicacion.plusDays(
                vacuna.getIntervaloRefuerzoDias());
    }

    public boolean debeRecordarse(LocalDate hoy) {
        if (hoy == null) {
            throw new IllegalArgumentException(
                    "La fecha de consulta es obligatoria");
        }

        LocalDate fechaRefuerzo = calcularProximoRefuerzo();
        LocalDate inicioAviso = fechaRefuerzo.minusDays(DIAS_AVISO_PREVIO);

        return !hoy.isBefore(inicioAviso);
    }

    public boolean estaVencida(LocalDate hoy) {
        if (hoy == null) {
            throw new IllegalArgumentException(
                    "La fecha de consulta es obligatoria");
        }

        return hoy.isAfter(
                calcularProximoRefuerzo().plusDays(DIAS_PARA_VENCER));
    }

    public Vacuna getVacuna() {
        return vacuna;
    }

    public String getLote() {
        return lote;
    }

    public LocalDate getFechaAplicacion() {
        return fechaAplicacion;
    }

    @Override
    public String toString() {
        return vacuna.getNombre()
                + " - lote: " + lote
                + " - aplicada el: " + fechaAplicacion
                + " - próximo refuerzo: " + calcularProximoRefuerzo();
    }
}

