
package modelo.entidades;

import java.time.LocalTime;

public class HorarioAtencion {
    private final DiaSemana diaSemana;
    private final LocalTime horaInicio;
    private final LocalTime horaFin;

    public HorarioAtencion(DiaSemana diaSemana, LocalTime horaInicio, LocalTime horaFin) {
        if (diaSemana == null || horaInicio == null || horaFin == null
                || !horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException("El horario de atención no es válido.");
        }
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }
    
    public boolean contiene(DiaSemana dia, LocalTime inicio, LocalTime fin) {
        return diaSemana == dia
                && !inicio.isBefore(horaInicio)
                && !fin.isAfter(horaFin);
    }
    public DiaSemana getDiaSemana() {
        return diaSemana;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }
    
    
}
