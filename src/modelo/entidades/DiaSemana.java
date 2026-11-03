
package modelo.entidades;

import java.time.DayOfWeek;


public enum DiaSemana {
    LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO;
    
    public static DiaSemana desdeFecha(DayOfWeek dia) {
        if (dia == null) {
            throw new IllegalArgumentException("El día no puede ser nulo");
        }
        return DiaSemana.valueOf(dia.name());
    }
}
