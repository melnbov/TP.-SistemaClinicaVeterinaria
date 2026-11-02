
package modelo.entidades;

import java.time.DayOfWeek;


public enum DiaSemana {
    LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO;
    
    public static DiaSemana desdeGecha(DayOfWeek dia){
        return DiaSemana.valueOf(dia.name());
    }

    static DiaSemana desdeFecha(DayOfWeek dayOfWeek) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
