package modelo.servicios;

import modelo.entidades.Dueno;
import modelo.entidades.EstadoTurno;
import modelo.entidades.Mascota;
import modelo.entidades.TipoAtencion;
import modelo.entidades.Turno;
import modelo.entidades.Veterinario;
import modelo.excepciones.ReglaNegocioException;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Gestiona los turnos de la clínica y sus reglas de negocio:
 * sin superposición por profesional, cancelación con cargo tardío,
 * seña tras tres ausencias seguidas y consultas de urgencia.
 */
public class ServicioTurnos implements Serializable {
    public static final int HORAS_CANCELACION_SIN_CARGO = 2;
    public static final double PORCENTAJE_CARGO_TARDIO = 0.20;
    public static final int AUSENCIAS_PARA_SENA = 3;
    private static final int PASO_MINUTOS = 15;
    private static final int DIAS_BUSQUEDA = 14;

    private final List<Turno> turnos = new ArrayList<>();

    /** Resultado de asignar una urgencia: el turno nuevo y los turnos que debieron moverse. */
    public static class ResultadoUrgencia {
        private final Turno urgente;
        private final List<Turno> desplazados;

        ResultadoUrgencia(Turno urgente, List<Turno> desplazados) {
            this.urgente = urgente;
            this.desplazados = desplazados;
        }

        public Turno getUrgente() { return urgente; }
        public List<Turno> getDesplazados() { return desplazados; }
    }

    public Turno asignarTurno(Mascota mascota, Veterinario veterinario,
                              LocalDateTime fechaHora, TipoAtencion tipo, boolean urgencia)
            throws ReglaNegocioException {
        return asignarTurno(mascota, veterinario, fechaHora, tipo, urgencia, false);
    }

    /** @param senaAbonada indica si el dueño pagó la seña exigida tras 3 ausencias seguidas */
    public Turno asignarTurno(Mascota mascota, Veterinario veterinario,
                              LocalDateTime fechaHora, TipoAtencion tipo, boolean urgencia,
                              boolean senaAbonada)
            throws ReglaNegocioException {
        if (mascota == null || veterinario == null || fechaHora == null || tipo == null) {
            throw new ReglaNegocioException("Todos los datos del turno son obligatorios");
        }
        if (requiereSena(mascota.getDuenoActual()) && !senaAbonada) {
            throw new ReglaNegocioException("El dueño acumula " + AUSENCIAS_PARA_SENA
                    + " ausencias seguidas: debe pagar una seña para pedir nuevos turnos");
        }
        if (!veterinario.estaDisponible(fechaHora, tipo.getDuracionMinutos())) {
            throw new ReglaNegocioException("El veterinario no atiende en ese horario");
        }
        if (haySuperposicion(veterinario, fechaHora, fechaHora.plusMinutes(tipo.getDuracionMinutos()), null)) {
            throw new ReglaNegocioException("El veterinario ya tiene un turno en ese horario");
        }
        Turno turno = crearTurno(mascota, veterinario, fechaHora, tipo, urgencia);
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

    /**
     * Cancela un turno. Hasta 2 horas antes no tiene cargo; después se cobra el 20 % de la consulta.
     * @param ahora momento de la cancelación (se recibe por parámetro para poder probarlo)
     * @return el cargo aplicado (0 si fue sin cargo)
     */
    public double cancelarTurno(int numero, LocalDateTime ahora) throws ReglaNegocioException {
        Turno turno = buscarPorNumero(numero);
        if (turno.getEstado() != EstadoTurno.PROGRAMADO) {
            throw new ReglaNegocioException("El turno no se encuentra programado");
        }
        if (ahora == null) {
            throw new ReglaNegocioException("La fecha de cancelación es obligatoria");
        }
        if (!ahora.isBefore(turno.getInicio())) {
            throw new ReglaNegocioException("El turno ya comenzó: debe registrarse como ausencia o atención");
        }
        boolean tardia = ahora.isAfter(turno.getInicio().minusHours(HORAS_CANCELACION_SIN_CARGO));
        double cargo = tardia ? turno.getAtencion().getPrecio() * PORCENTAJE_CARGO_TARDIO : 0;
        turno.cancelar(cargo);
        return cargo;
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

    /** Un dueño con tres ausencias seguidas (sus últimos turnos cerrados) debe pagar una seña. */
    public boolean requiereSena(Dueno dueno) {
        if (dueno == null) return false;
        List<Turno> cerrados = new ArrayList<>();
        for (Turno t : turnos) {
            boolean cerrado = t.getEstado() == EstadoTurno.ATENDIDO || t.getEstado() == EstadoTurno.AUSENTE;
            if (cerrado && dueno.equals(t.getMascota().getDuenoActual())) cerrados.add(t);
        }
        cerrados.sort(Comparator.<Turno>naturalOrder().reversed());
        int seguidas = 0;
        for (Turno t : cerrados) {
            if (t.getEstado() != EstadoTurno.AUSENTE) break;
            seguidas++;
        }
        return seguidas >= AUSENCIAS_PARA_SENA;
    }

    /**
     * Urgencia: toma el primer horario libre del profesional de guardia. Si está ocupado por turnos
     * comunes, los desplaza al siguiente horario libre y avisa al dueño por el notificador.
     */
    public ResultadoUrgencia asignarUrgencia(Mascota mascota, Veterinario guardia,
                                             LocalDateTime ahora, Notificador notificador)
            throws ReglaNegocioException {
        if (mascota == null || guardia == null || ahora == null) {
            throw new ReglaNegocioException("Mascota, profesional de guardia y fecha son obligatorios");
        }
        if (!guardia.isDeGuardia()) {
            throw new ReglaNegocioException("El veterinario seleccionado no está de guardia");
        }
        TipoAtencion tipo = TipoAtencion.CONSULTA_GENERAL;
        int duracion = tipo.getDuracionMinutos();
        LocalDateTime inicio = redondearArriba(ahora.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1));
        LocalDateTime elegido = null;
        for (int i = 0; i < DIAS_BUSQUEDA * 24 * 60 / PASO_MINUTOS && elegido == null; i++) {
            LocalDateTime t = inicio.plusMinutes((long) i * PASO_MINUTOS);
            if (guardia.estaDisponible(t, duracion) && !superponeUrgencia(guardia, t, t.plusMinutes(duracion))) {
                elegido = t;
            }
        }
        if (elegido == null) {
            throw new ReglaNegocioException("No hay horarios disponibles para la urgencia");
        }
        Turno urgente = crearTurno(mascota, guardia, elegido, tipo, true);
        List<Turno> aMover = turnosSolapados(guardia, urgente.getInicio(), urgente.getFin());
        List<LocalDateTime> originales = new ArrayList<>();
        for (Turno t : aMover) originales.add(t.getInicio());

        turnos.add(urgente);
        try {
            for (Turno t : aMover) {
                LocalDateTime libre = buscarHorarioLibre(guardia, urgente.getFin(), t.getDuracionMinutos(), t);
                if (libre == null) throw new ReglaNegocioException("No se pudo reprogramar el turno N° " + t.getNumero());
                t.cambiarFechaHora(libre);
            }
        } catch (ReglaNegocioException | RuntimeException ex) {
            // Se deshace todo: la urgencia no se asigna y los turnos vuelven a su lugar.
            turnos.remove(urgente);
            for (int i = 0; i < aMover.size(); i++) aMover.get(i).restaurarFechaHora(originales.get(i));
            throw ex instanceof ReglaNegocioException ? (ReglaNegocioException) ex
                    : new ReglaNegocioException(ex.getMessage());
        }
        if (notificador != null) {
            for (Turno t : aMover) {
                notificador.enviarAviso(t.getMascota().getDuenoActual(),
                        "Su turno N° " + t.getNumero() + " fue reprogramado por una urgencia. Nuevo horario: " + t.getInicio());
            }
        }
        return new ResultadoUrgencia(urgente, aMover);
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

    /** Mayor número de turno existente (se usa para restaurar el contador al cargar datos). */
    public int getUltimoNumero() {
        int max = 0;
        for (Turno t : turnos) max = Math.max(max, t.getNumero());
        return max;
    }

    private Turno crearTurno(Mascota m, Veterinario v, LocalDateTime f, TipoAtencion tipo, boolean urgencia)
            throws ReglaNegocioException {
        try {
            return new Turno(m, v, f, tipo, urgencia);
        } catch (IllegalArgumentException ex) {
            throw new ReglaNegocioException(ex.getMessage());
        }
    }

    private LocalDateTime buscarHorarioLibre(Veterinario v, LocalDateTime desde, int duracion, Turno excluir) {
        LocalDateTime t = redondearArriba(desde);
        for (int i = 0; i < DIAS_BUSQUEDA * 24 * 60 / PASO_MINUTOS; i++, t = t.plusMinutes(PASO_MINUTOS)) {
            if (v.estaDisponible(t, duracion) && !haySuperposicion(v, t, t.plusMinutes(duracion), excluir)) return t;
        }
        return null;
    }

    private LocalDateTime redondearArriba(LocalDateTime t) {
        LocalDateTime base = t.truncatedTo(ChronoUnit.MINUTES);
        int resto = base.getMinute() % PASO_MINUTOS;
        if (resto != 0 || base.isBefore(t)) base = base.plusMinutes(PASO_MINUTOS - resto);
        return base;
    }

    private boolean superponeUrgencia(Veterinario v, LocalDateTime inicio, LocalDateTime fin) {
        for (Turno t : turnosSolapados(v, inicio, fin)) if (t.isEsUrgencia()) return true;
        return false;
    }

    private List<Turno> turnosSolapados(Veterinario v, LocalDateTime inicio, LocalDateTime fin) {
        List<Turno> r = new ArrayList<>();
        for (Turno e : turnos) {
            if (e.getVeterinario().equals(v) && e.getEstado() == EstadoTurno.PROGRAMADO
                    && inicio.isBefore(e.getFin()) && e.getInicio().isBefore(fin)) r.add(e);
        }
        r.sort(Comparator.naturalOrder());
        return r;
    }

    private boolean haySuperposicion(Veterinario veterinario, LocalDateTime inicio,
                                     LocalDateTime fin, Turno excluir) {
        for (Turno existente : turnos) {
            if (existente == excluir || !existente.getVeterinario().equals(veterinario)
                    || existente.getEstado() != EstadoTurno.PROGRAMADO) continue;
            if (inicio.isBefore(existente.getFin()) && existente.getInicio().isBefore(fin)) return true;
        }
        return false;
    }
}
