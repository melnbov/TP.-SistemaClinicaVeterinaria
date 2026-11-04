package modelo.servicios;

import modelo.entidades.EstadoTurno;
import modelo.entidades.Mascota;
import modelo.entidades.TipoAtencion;
import modelo.entidades.Turno;
import modelo.entidades.Veterinario;
import modelo.excepciones.ReglaNegocioException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ServicioTurnos {
    private final List<Turno> turnos = new ArrayList<>();

    public Turno asignarTurno(Mascota mascota, Veterinario veterinario,
                              LocalDateTime fechaHora, TipoAtencion tipo, boolean urgencia)
            throws ReglaNegocioException {
        if (mascota == null || veterinario == null || fechaHora == null || tipo == null) {
            throw new ReglaNegocioException("Todos los datos del turno son obligatorios");
        }
        if (!veterinario.estaDisponible(fechaHora, tipo.getDuracionMinutos())) {
            throw new ReglaNegocioException("El veterinario no atiende en ese horario");
        }
        if (haySuperposicion(veterinario, fechaHora, fechaHora.plusMinutes(tipo.getDuracionMinutos()), null)) {
            throw new ReglaNegocioException("El veterinario ya tiene un turno en ese horario");
        }
        Turno turno = new Turno(mascota, veterinario, fechaHora, tipo, urgencia);
        turnos.add(turno);
        return turno;
    }

    public void reprogramarTurno(int numero, LocalDateTime nuevaFechaHora) throws ReglaNegocioException {
        Turno turno = buscarPorNumero(numero);
        if (turno.getEstado() != EstadoTurno.PROGRAMADO) {
            throw new ReglaNegocioException("Solo se pueden reprogramar turnos programados");
        }
        if (nuevaFechaHora == null || !turno.getVeterinario().estaDisponible(nuevaFechaHora, turno.getDuracionMinutos())) {
            throw new ReglaNegocioException("El veterinario no está disponible en ese horario");
        }
        if (haySuperposicion(turno.getVeterinario(), nuevaFechaHora,
                nuevaFechaHora.plusMinutes(turno.getDuracionMinutos()), turno)) {
            throw new ReglaNegocioException("El nuevo horario se superpone con otro turno");
        }
        try { turno.cambiarFechaHora(nuevaFechaHora); }
        catch (IllegalArgumentException ex) { throw new ReglaNegocioException(ex.getMessage()); }
    }

    public void cancelarTurno(int numero) throws ReglaNegocioException {
        Turno turno = buscarPorNumero(numero);
        if (turno.getEstado() != EstadoTurno.PROGRAMADO) {
            throw new ReglaNegocioException("El turno no se encuentra programado");
        }
        turno.cambiarEstado(EstadoTurno.CANCELADO);
    }

    public void registrarAusencia(int numero) throws ReglaNegocioException {
        Turno turno = buscarPorNumero(numero);
        if (turno.getEstado() != EstadoTurno.PROGRAMADO) {
            throw new ReglaNegocioException("El turno no se encuentra programado");
        }
        turno.cambiarEstado(EstadoTurno.AUSENTE);
    }

    public void registrarAtencion(int numero) throws ReglaNegocioException {
        Turno turno = buscarPorNumero(numero);
        if (turno.getEstado() != EstadoTurno.PROGRAMADO) {
            throw new ReglaNegocioException("El turno no se encuentra programado");
        }
        turno.cambiarEstado(EstadoTurno.ATENDIDO);
    }

    public Turno buscarPorNumero(int numero) throws ReglaNegocioException {
        for (Turno turno : turnos) {
            if (turno.getNumero() == numero) return turno;
        }
        throw new ReglaNegocioException("No existe un turno con el número " + numero);
    }

    public List<Turno> listarTurnos() {
        List<Turno> copia = new ArrayList<>(turnos);
        Collections.sort(copia);
        return copia;
    }

    public List<Turno> listarTurnosPorVeterinario(Veterinario veterinario) {
        List<Turno> resultado = new ArrayList<>();
        if (veterinario == null) return resultado;
        for (Turno turno : turnos) if (turno.getVeterinario().equals(veterinario)) resultado.add(turno);
        resultado.sort(Comparator.naturalOrder());
        return resultado;
    }

    private boolean haySuperposicion(Veterinario veterinario, LocalDateTime inicio,
                                     LocalDateTime fin, Turno excluir) {
        for (Turno existente : turnos) {
            if (existente == excluir || existente.getVeterinario() != veterinario
                    || existente.getEstado() != EstadoTurno.PROGRAMADO) continue;
            if (inicio.isBefore(existente.getFin()) && existente.getInicio().isBefore(fin)) return true;
        }
        return false;
    }
}
