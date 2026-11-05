
package modelo.entidades;

import java.time.LocalDateTime;

public class Turno implements Comparable<Turno>, java.io.Serializable {
    private static int siguienteNumero=1;
    
    private final int numero;
    private Mascota mascota;
    private Veterinario veterinario;
    private LocalDateTime fechaHora;
    private TipoAtencion atencion;
    private EstadoTurno estado;
    private final boolean esUrgencia;
    private final int duracionMinutos;
    private double cargoCancelacion; // 20 % si se cancela con menos de 2 h de anticipación

    public Turno(Mascota mascota, Veterinario veterinario, LocalDateTime fechaHora, TipoAtencion atencion, boolean esUrgencia) {
        if (mascota==null || veterinario == null || fechaHora==null || atencion==null){
            throw new IllegalArgumentException("Mascota, veterinario, fecha y tipo de atención son obligatorios");
        }
        if (fechaHora.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("No se puede crear un turno en el pasado");
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
    
    public void cambiarFechaHora(LocalDateTime nuevaFechaHora) {
        if (nuevaFechaHora == null || nuevaFechaHora.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La nueva fecha y hora no son válidas");
        }
        this.fechaHora = nuevaFechaHora;
    }

    public double getCargoCancelacion() {
        return cargoCancelacion;
    }

    /** Cancela el turno registrando el cargo (puede ser 0). */
    public void cancelar(double cargo) {
        if (cargo < 0) throw new IllegalArgumentException("El cargo no puede ser negativo");
        this.cargoCancelacion = cargo;
        this.estado = EstadoTurno.CANCELADO;
    }

    /** Vuelve a un horario anterior sin validaciones (solo para deshacer un desplazamiento fallido). */
    public void restaurarFechaHora(LocalDateTime anterior) {
        this.fechaHora = anterior;
    }

    public void cambiarEstado(EstadoTurno nuevoEstado) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El estado no puede ser nulo");
        }
        this.estado = nuevoEstado;
    }

    public static void actualizarSecuencia(int ultimoNumero) {
        if (ultimoNumero < 0) throw new IllegalArgumentException("Número inválido");
        siguienteNumero = Math.max(siguienteNumero, ultimoNumero + 1);
    }
    
    @Override
    public int compareTo(Turno otro){
        return this.fechaHora.compareTo(otro.fechaHora);
    }
    
    @Override
    public String toString() {
        return "Turno N° " + numero + " | " + fechaHora + " | " + atencion + (esUrgencia ? " (URGENCIA)" : "")
                + " | " + mascota.getNombre() + " | Dr/a. " + veterinario.getNombreCompleto() + " | " + estado;
    }
    
}
