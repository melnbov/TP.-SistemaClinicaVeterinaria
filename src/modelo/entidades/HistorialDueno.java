package modelo.entidades;

import modelo.excepciones.ReglaNegocioException;
import java.time.LocalDate;

public class HistorialDueno {

    private final Dueno dueno;
    private final LocalDate fechaDesde;
    private LocalDate fechaHasta; // null significa que la tenencia sigue vigente

    public HistorialDueno(Dueno dueno, LocalDate fechaDesde) {
        if (dueno == null) {
            throw new IllegalArgumentException("El dueño es obligatorio");
        }

        if (fechaDesde == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria");
        }
        if (fechaDesde.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La fecha de inicio no puede ser futura");
        }

        this.dueno = dueno;
        this.fechaDesde = fechaDesde;
    }

    public void cerrar(LocalDate fechaHasta) throws ReglaNegocioException {
        if (fechaHasta == null) {
            throw new ReglaNegocioException("La fecha de fin es obligatoria");
        }

        if (this.fechaHasta != null) {
            throw new ReglaNegocioException("La tenencia ya estaba cerrada");
        }

        if (fechaHasta.isBefore(fechaDesde)) {
            throw new ReglaNegocioException(
                    "La fecha de fin no puede ser anterior a la fecha de inicio");
        }

        if (fechaHasta.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException(
                    "La fecha de fin no puede ser futura");
        }

        this.fechaHasta = fechaHasta;
    }

    public boolean estaVigente() {
        return fechaHasta == null;
    }

    public Dueno getDueno() {
        return dueno;
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    @Override
    public String toString() {
        if (fechaHasta == null) {
            return dueno + " desde " + fechaDesde + " (vigente)";
        }

        return dueno + " desde " + fechaDesde + " hasta " + fechaHasta;
    }
}
