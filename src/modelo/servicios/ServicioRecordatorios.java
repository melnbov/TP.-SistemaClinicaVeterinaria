package modelo.servicios;
import modelo.entidades.*;

import java.io.Serializable;
import java.time.LocalDate; import java.util.ArrayList; import java.util.List;
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

    /** Refuerzo de una vacuna que se acerca, junto con la mascota a la que corresponde. */
    public static final class RefuerzoProximo {
        private final Mascota mascota;
        private final VacunaAplicada vacuna;
        RefuerzoProximo(Mascota mascota,VacunaAplicada vacuna){
            this.mascota=mascota;
            this.vacuna=vacuna;}
        public Mascota getMascota(){return mascota;}
        public VacunaAplicada getVacuna(){return vacuna;}
    }

    /** Refuerzos cuya fecha cae entre {@code hoy} y {@code hoy + dias} (ambos inclusive). */
    public List<RefuerzoProximo> consultarRefuerzosProximos(List<Mascota> mascotas,LocalDate hoy,int dias){
        if (mascotas == null || hoy == null){
            throw new IllegalArgumentException("Mascotas y fecha son obligatorias");
        }
        List<RefuerzoProximo> r=new ArrayList<>();
        for(Mascota m:mascotas)
            for(Consulta c:m.getHistoriaClinica())
                for(VacunaAplicada v:c.getVacunasAplicadas()){
                    LocalDate refuerzo=v.calcularProximoRefuerzo();
                    if(!refuerzo.isBefore(hoy)&&!refuerzo.isAfter(hoy.plusDays(dias)))
                        r.add(new RefuerzoProximo(m,v));
                }
        return r;
    }
}



