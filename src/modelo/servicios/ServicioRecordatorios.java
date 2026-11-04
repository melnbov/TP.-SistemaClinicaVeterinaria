package modelo.servicios;
import modelo.entidades.*;

import java.io.Serializable;
import java.time.LocalDate; import java.util.List;
public class ServicioRecordatorios implements Serializable {
    private final Notificador notificador;
    public ServicioRecordatorios(Notificador notificador){
        if(notificador==null)
            throw new IllegalArgumentException("Notificador obligatorio");
        this.notificador=notificador;}
    public void avisarDueno(Dueno dueno,String mensaje){
        notificador.enviarAviso(dueno,mensaje);
    }
    public void avisarVacunasProximas(List<Mascota> mascotas,LocalDate hoy){
        if (mascotas == null || hoy == null){
            throw new IllegalArgumentException("Mascotas y fecha son obligatorias");
        }
        for(Mascota mascota:mascotas)
            for (VacunaAplicada vacuna : mascota.getVacunasPendientesDeRecordatorio(hoy)) {
                notificador.enviarAviso(
                        mascota.getDuenoActual(),
                        "El refuerzo de " + vacuna.getVacuna().getNombre()
                                + " para " + mascota.getNombre()
                                + " vence el " + vacuna.calcularProximoRefuerzo());
            }
    }
}



