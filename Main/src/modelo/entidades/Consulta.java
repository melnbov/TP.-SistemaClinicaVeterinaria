
package modelo.entidades;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Consulta {
    //atributos
    private final int numero;
    private final Turno turno;
    private final Veterinario veterinario;
    private final LocalDateTime fecha;
    private final String motivo;
    private final double pesoDelDia;
    private final String diagnostico;
    private final String observaciones;
    private boolean cerrada;
    private final List<Tratamiento> tratamientos = new ArrayList<>();
    
    //constrcutor

    public Consulta(int numero, Turno turno, Veterinario veterinario, String motivo, double pesoDelDia, String diagnostico, String observaciones) {
        if (turno == null || motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Turno y motivo son obligatorios.");
        }
        if (turno.getEstado() != EstadoTurno.ATENDIDO) {
            throw new IllegalArgumentException("Para registrar la consulta, el turno debe estar atendido.");
        }
        if (pesoDelDia < 0) {
            throw new IllegalArgumentException("El peso no puede ser negativo.");
        }
        this.numero = numero;
        this.turno = turno;
        this.fecha=turno.getInicio();
        this.veterinario = turno.getVeterinario();
        this.motivo = motivo;
        this.pesoDelDia = pesoDelDia;
        this.diagnostico = diagnostico;
        this.observaciones = observaciones;
    }
    
    public void indicarTratamiento(Tratamiento tratamiento) {
        if (tratamiento == null) {
            throw new IllegalArgumentException("El tratamiento no puede ser nulo.");
        }
        if (pesoDelDia <= 0) {
            throw new IllegalStateException("Debe registrar el peso de la mascota antes de indicar medicamentos.");
        }
        tratamientos.add(tratamiento);
    }
    public int getNumero() {
        return numero;
    }

    public Turno getTurno() {
        return turno;
    }
    
    public Veterinario getVeterinario() {
        return veterinario;
    }
    
    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getMotivo() {
        return motivo;
    }

    public double getPesoDelDia() {
        return pesoDelDia;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public List<Tratamiento> getTratamientos() {
        return List.copyOf(tratamientos);
    }
    
    
}
