
package modelo.entidades;

import java.time.LocalDateTime;

public class Turno implements Comparable<Turno>{
    private static int siguienteNumero=1;
    
    private final int numero;
    private Mascota mascota;
    private Veterinario veterinario;
    private LocalDateTime fechaHora;
    private TipoAtencion atencion;
    private EstadoTurno estado;
    private final boolean esUrgencia;
    private final int duracionMinutos;

    public Turno(Mascota mascota, Veterinario veterinario, LocalDateTime fechaHora, TipoAtencion atencion, boolean esUrgencia) {
        if (mascota==null || veterinario == null || fechaHora==null || atencion==null){
            throw new IllegalArgumentException("Mascota, veterinario, fecha y tipo de atención son obligatorios");
        }
        this.numero=siguienteNumero++;
        this.mascota = mascota;
        this.veterinario = veterinario;
        this.fechaHora = fechaHora;
        this.atencion = atencion;
        this.esUrgencia = esUrgencia;
        this.duracionMinutos=atencion.getDuracionMinutos();
        this.estado=EstadoTurno.PROGRAMADO;
                
    }

    public LocalDateTime getInicio(){
        return fechaHora;
    }
    
    public LocalDateTime getFin(){
        return fechaHora.plusMinutes(duracionMinutos);
    }
    
    public Mascota getMascota() {
        return mascota;
    }

    public Veterinario getVeterinario() {
        return veterinario;
    }


    public TipoAtencion getAtencion() {
        return atencion;
    }

    public EstadoTurno getEstado() {
        return estado;
    }

    public boolean isEsUrgencia() {
        return esUrgencia;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public int getNumero() {
        return numero;
    }
    
    void cambiarFechaHora(LocalDateTime nuevaFechaHora) { 
        this.fechaHora = nuevaFechaHora; 
    }
    void cambiarEstado(EstadoTurno nuevoEstado) { 
        this.estado = nuevoEstado; 
    }
    
    @Override
    public int compareTo(Turno otro){
        return this.fechaHora.compareTo(otro.fechaHora);
    }
    
    @Override
    public String toString() {
        return "Turno N° " + numero + " | " + fechaHora + " | " + atencion + " | "+veterinario.getNombreCompleto() + " | " + estado;
    }
    
}
