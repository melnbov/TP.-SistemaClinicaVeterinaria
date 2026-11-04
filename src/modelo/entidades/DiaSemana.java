
package modelo.entidades;

import java.time.DayOfWeek;


public enum DiaSemana {
    LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO;
    
    public static DiaSemana desdeFecha(DayOfWeek dia) {
        if (dia == null) {
            throw new IllegalArgumentException("El día no puede ser nulo");
        }
        // Los nombres de java.time.DayOfWeek están en inglés: se mapea por posición (lunes = 1).
        return DiaSemana.values()[dia.getValue() - 1];
    }
}
