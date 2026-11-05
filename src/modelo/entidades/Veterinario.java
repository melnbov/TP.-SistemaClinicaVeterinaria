
package modelo.entidades;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class Veterinario extends Personal{
    private final String matricula;
    private final String especialidad;
    private boolean deGuardia;
    private final List<HorarioAtencion> horarios= new ArrayList<>();
    
    //constructor
    public Veterinario(String nombre, String apellido, String documento, String telefono, String matricula, String especialidad) {
        super(nombre, apellido, documento, telefono);
        if (matricula == null || matricula.isBlank()) {
            throw new IllegalArgumentException("La matrícula es obligatoria.");
        }
        this.matricula = matricula;
        this.especialidad = especialidad;
    }
    public void agregarHorario(HorarioAtencion horario){
        if (horario == null) throw new IllegalArgumentException("El horario no puede ser nulo");
        horarios.add(horario);
    }
    
    public boolean estaDisponible(LocalDateTime inicio, int duracionMinutos) {
        if (inicio == null || duracionMinutos <= 0) return false;
        LocalDateTime fin = inicio.plusMinutes(duracionMinutos);
        DiaSemana dia = DiaSemana.desdeFecha(inicio.getDayOfWeek());

        for (HorarioAtencion horario : horarios) {
            if (horario.contiene(dia, inicio.toLocalTime(), fin.toLocalTime())) {
                return true;
            }
        }
        return false;
    }
    public boolean isDeGuardia() {
        return deGuardia;
    }

    public void setDeGuardia(boolean deGuardia) {
        this.deGuardia = deGuardia;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Veterinario)) return false;
        return matricula.equals(((Veterinario) o).matricula);
    }

    @Override
    public int hashCode() {
        return matricula.hashCode();
    }

    @Override
    public String toString() {
        return getNombreCompleto() + " (Mat. " + matricula + ", " + especialidad + ")";
    }

    public String getMatricula() {
        return matricula;
    }

    public String getEspecialidad() {
        return especialidad;
    }
    
    public List<HorarioAtencion> getHorarios(){
        return List.copyOf(horarios);
    }
    
    @Override
    public String getRol() {
        return "Veterinario";
    }
}
