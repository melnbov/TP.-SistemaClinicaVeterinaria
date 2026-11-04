package modelo.servicios;
import modelo.entidades.*; import java.time.LocalDate; import java.util.List;
public class ReporteVacunasProximas implements Reporte {
    private final List<Mascota> mascotas;
    private final LocalDate fecha;
    public ReporteVacunasProximas(List<Mascota> mascotas,LocalDate fecha){
        this.mascotas=List.copyOf(mascotas);
        if(fecha==null)
            throw new IllegalArgumentException("Fecha obligatoria");
        this.fecha=fecha;
    }
    public String generarReporte(){
        StringBuilder s=new StringBuilder("REPORTE DE VACUNAS / REFUERZOS\n");
        for(Mascota m:mascotas)for(Consulta c:m.getHistoriaClinica())
            for(VacunaAplicada v:c.getVacunasAplicadas())
                if(!v.calcularProximoRefuerzo().isBefore(fecha)&&!v.calcularProximoRefuerzo().isAfter(fecha.plusDays(30)))s.append(m.getNombre()).append(" - ").append(v.getVacuna().getNombre()).append(" - ").append(v.calcularProximoRefuerzo()).append('\n');return s.toString();
    }
}
