package modelo.servicios;
import modelo.entidades.Turno; import java.util.List;
public class ReporteTurnosVeterinario implements Reporte {
    private final List<Turno> turnos;
    public ReporteTurnosVeterinario(List<Turno> turnos){
        this.turnos=List.copyOf(turnos);
    }
    public String generarReporte(){
        StringBuilder s=new StringBuilder("REPORTE DE TURNOS\n");
        for(Turno t:turnos)s.append(t).append('\n');
        return s.toString();
    }
}

