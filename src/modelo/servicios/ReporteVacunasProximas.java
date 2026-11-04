package modelo.servicios;
import modelo.entidades.Mascota;
import java.time.LocalDate;
import java.util.List;

/** Reporte de refuerzos próximos: la ventana y el cálculo los hace {@link ServicioRecordatorios}. */
public class ReporteVacunasProximas implements Reporte {
    private static final int DIAS_VENTANA = 30;
    private final ServicioRecordatorios servicioRecordatorios;
    private final List<Mascota> mascotas;
    private final LocalDate fecha;
    public ReporteVacunasProximas(ServicioRecordatorios servicioRecordatorios,List<Mascota> mascotas,LocalDate fecha){
        if(servicioRecordatorios==null)
            throw new IllegalArgumentException("ServicioRecordatorios obligatorio");
        if(mascotas==null)
            throw new IllegalArgumentException("Mascotas obligatorias");
        if(fecha==null)
            throw new IllegalArgumentException("Fecha obligatoria");
        this.servicioRecordatorios=servicioRecordatorios;
        this.mascotas=List.copyOf(mascotas);
        this.fecha=fecha;
    }
    public String generarReporte(){
        StringBuilder s=new StringBuilder("REPORTE DE VACUNAS / REFUERZOS\n");
        for(ServicioRecordatorios.RefuerzoProximo r:servicioRecordatorios.consultarRefuerzosProximos(mascotas,fecha,DIAS_VENTANA))
            s.append(r.getMascota().getNombre()).append(" - ").append(r.getVacuna().getVacuna().getNombre())
             .append(" - ").append(r.getVacuna().calcularProximoRefuerzo()).append('\n');
        return s.toString();
    }
}
