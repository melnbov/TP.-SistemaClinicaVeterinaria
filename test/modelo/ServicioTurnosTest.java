package modelo;

import modelo.entidades.*;
import modelo.excepciones.ReglaNegocioException;
import modelo.servicios.Notificador;
import modelo.servicios.ServicioTurnos;
import org.junit.Before;
import org.junit.Test;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ServicioTurnosTest {
    private ServicioTurnos servicio;
    private Veterinario vet;
    private Dueno dueno;
    private Mascota rex;
    private Mascota mishi;
    private LocalDate lunes;

    @Before
    public void preparar() {
        servicio = new ServicioTurnos();
        vet = new Veterinario("Ana", "Gómez", "1", "1", "M-1", "General");
        vet.setDeGuardia(true);
        for (DiaSemana d : DiaSemana.values()) {
            if (d != DiaSemana.DOMINGO) vet.agregarHorario(new HorarioAtencion(d, LocalTime.of(9, 0), LocalTime.of(19, 0)));
        }
        dueno = new Dueno("Juan", "Pérez", "100", "555", "j@x.com");
        rex = new Perro("Rex", "Mestizo", "M", LocalDate.now().minusYears(3), 10, dueno, "CHIP1", 40);
        mishi = new Gato("Mishi", "Siamés", "H", LocalDate.now().minusYears(2), 4, dueno, EstiloVida.INTERIOR);
        lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(1);
    }

    private LocalDateTime lunesA(int h, int m) { return lunes.atTime(h, m); }

    @Test
    public void asignaTurnoValido() throws Exception {
        Turno t = servicio.asignarTurno(rex, vet, lunesA(10, 0), TipoAtencion.CONSULTA_GENERAL, false);
        assertEquals(EstadoTurno.PROGRAMADO, t.getEstado());
        assertEquals(lunesA(10, 30), t.getFin());
    }

    @Test(expected = ReglaNegocioException.class)
    public void rechazaSuperposicionDelMismoProfesional() throws Exception {
        servicio.asignarTurno(rex, vet, lunesA(10, 0), TipoAtencion.CONSULTA_GENERAL, false);
        servicio.asignarTurno(mishi, vet, lunesA(10, 15), TipoAtencion.VACUNACION, false);
    }

    @Test
    public void turnoContiguoNoEsSuperposicion() throws Exception {
        servicio.asignarTurno(rex, vet, lunesA(10, 0), TipoAtencion.CONSULTA_GENERAL, false);
        servicio.asignarTurno(mishi, vet, lunesA(10, 30), TipoAtencion.CONSULTA_GENERAL, false);
        assertEquals(2, servicio.listarTurnos().size());
    }

    @Test(expected = ReglaNegocioException.class)
    public void rechazaTurnoFueraDelHorarioDelProfesional() throws Exception {
        servicio.asignarTurno(rex, vet, lunesA(18, 45), TipoAtencion.CONSULTA_GENERAL, false); // termina 19:15
    }

    @Test(expected = ReglaNegocioException.class)
    public void rechazaTurnoEnDiaSinAtencion() throws Exception {
        servicio.asignarTurno(rex, vet, lunes.plusDays(6).atTime(10, 0), TipoAtencion.CONSULTA_GENERAL, false); // domingo
    }

    @Test
    public void duracionDependeDelTipoDeAtencion() {
        assertEquals(30, TipoAtencion.CONSULTA_GENERAL.getDuracionMinutos());
        assertEquals(15, TipoAtencion.VACUNACION.getDuracionMinutos());
        assertEquals(120, TipoAtencion.CIRUGIA.getDuracionMinutos());
    }

    @Test
    public void cancelarConMasDeDosHorasNoTieneCargo() throws Exception {
        Turno t = servicio.asignarTurno(rex, vet, lunesA(12, 0), TipoAtencion.CONSULTA_GENERAL, false);
        double cargo = servicio.cancelarTurno(t.getNumero(), lunesA(9, 59));
        assertEquals(0.0, cargo, 0.001);
        assertEquals(EstadoTurno.CANCELADO, t.getEstado());
    }

    @Test
    public void cancelarConMenosDeDosHorasCobraVeintePorCiento() throws Exception {
        Turno t = servicio.asignarTurno(rex, vet, lunesA(12, 0), TipoAtencion.CONSULTA_GENERAL, false);
        double cargo = servicio.cancelarTurno(t.getNumero(), lunesA(10, 30));
        assertEquals(TipoAtencion.CONSULTA_GENERAL.getPrecio() * 0.20, cargo, 0.001);
        assertEquals(cargo, t.getCargoCancelacion(), 0.001);
    }

    @Test
    public void justoDosHorasAntesNoTieneCargo() throws Exception {
        Turno t = servicio.asignarTurno(rex, vet, lunesA(12, 0), TipoAtencion.CONSULTA_GENERAL, false);
        assertEquals(0.0, servicio.cancelarTurno(t.getNumero(), lunesA(10, 0)), 0.001);
    }

    @Test(expected = ReglaNegocioException.class)
    public void noSePuedeCancelarDosVeces() throws Exception {
        Turno t = servicio.asignarTurno(rex, vet, lunesA(12, 0), TipoAtencion.CONSULTA_GENERAL, false);
        servicio.cancelarTurno(t.getNumero(), lunesA(8, 0));
        servicio.cancelarTurno(t.getNumero(), lunesA(8, 5));
    }

    @Test
    public void reprogramaSinSuperponer() throws Exception {
        Turno a = servicio.asignarTurno(rex, vet, lunesA(10, 0), TipoAtencion.CONSULTA_GENERAL, false);
        servicio.asignarTurno(mishi, vet, lunesA(11, 0), TipoAtencion.CONSULTA_GENERAL, false);
        servicio.reprogramarTurno(a.getNumero(), lunesA(15, 0));
        assertEquals(lunesA(15, 0), a.getInicio());
    }

    @Test(expected = ReglaNegocioException.class)
    public void reprogramarSobreOtroTurnoSeRechaza() throws Exception {
        Turno a = servicio.asignarTurno(rex, vet, lunesA(10, 0), TipoAtencion.CONSULTA_GENERAL, false);
        servicio.asignarTurno(mishi, vet, lunesA(11, 0), TipoAtencion.CONSULTA_GENERAL, false);
        servicio.reprogramarTurno(a.getNumero(), lunesA(11, 15));
    }

    @Test
    public void tresAusenciasSeguidasExigenSena() throws Exception {
        for (int i = 0; i < 3; i++) {
            Turno t = servicio.asignarTurno(rex, vet, lunes.plusDays(i).atTime(10, 0), TipoAtencion.CONSULTA_GENERAL, false);
            servicio.registrarAusencia(t.getNumero());
        }
        assertTrue(servicio.requiereSena(dueno));
        try {
            servicio.asignarTurno(rex, vet, lunes.plusDays(4).atTime(10, 0), TipoAtencion.CONSULTA_GENERAL, false);
            fail("Debía exigir seña");
        } catch (ReglaNegocioException esperado) {
            assertTrue(esperado.getMessage().contains("seña"));
        }
        Turno conSena = servicio.asignarTurno(rex, vet, lunes.plusDays(4).atTime(10, 0), TipoAtencion.CONSULTA_GENERAL, false, true);
        assertNotNull(conSena);
    }

    @Test
    public void dosAusenciasNoExigenSena() throws Exception {
        for (int i = 0; i < 2; i++) {
            Turno t = servicio.asignarTurno(rex, vet, lunes.plusDays(i).atTime(10, 0), TipoAtencion.CONSULTA_GENERAL, false);
            servicio.registrarAusencia(t.getNumero());
        }
        assertFalse(servicio.requiereSena(dueno));
    }

    @Test
    public void unaAtencionEntreMedioCortaLaRacha() throws Exception {
        Turno a1 = servicio.asignarTurno(rex, vet, lunes.atTime(10, 0), TipoAtencion.CONSULTA_GENERAL, false);
        Turno a2 = servicio.asignarTurno(rex, vet, lunes.plusDays(1).atTime(10, 0), TipoAtencion.CONSULTA_GENERAL, false);
        Turno at = servicio.asignarTurno(rex, vet, lunes.plusDays(2).atTime(10, 0), TipoAtencion.CONSULTA_GENERAL, false);
        Turno a3 = servicio.asignarTurno(rex, vet, lunes.plusDays(3).atTime(10, 0), TipoAtencion.CONSULTA_GENERAL, false);
        servicio.registrarAusencia(a1.getNumero());
        servicio.registrarAusencia(a2.getNumero());
        servicio.registrarAtencion(at.getNumero());
        servicio.registrarAusencia(a3.getNumero());
        assertFalse(servicio.requiereSena(dueno));
    }

    /** Notificador de prueba que guarda los avisos en una lista. */
    private static class NotificadorFalso implements Notificador {
        final List<String> avisos = new ArrayList<>();
        public void enviarAviso(Dueno d, String mensaje) { avisos.add(d.getEmail() + ": " + mensaje); }
    }

    @Test
    public void urgenciaTomaElPrimerHorarioLibreDelProfesionalDeGuardia() throws Exception {
        NotificadorFalso n = new NotificadorFalso();
        ServicioTurnos.ResultadoUrgencia r = servicio.asignarUrgencia(rex, vet, lunesA(7, 0), n);
        assertEquals(lunesA(9, 0), r.getUrgente().getInicio());
        assertTrue(r.getUrgente().isEsUrgencia());
        assertTrue(r.getDesplazados().isEmpty());
        assertTrue(n.avisos.isEmpty());
    }

    @Test
    public void urgenciaDesplazaYNotificaAlTurnoQueEstabaOcupandoElHorario() throws Exception {
        Dueno otro = new Dueno("Ana", "López", "200", "444", "ana@x.com");
        Mascota firu = new Conejo("Firu", "Enano", "M", LocalDate.now().minusYears(1), 2, otro);
        Turno comun = servicio.asignarTurno(firu, vet, lunesA(9, 0), TipoAtencion.CONSULTA_GENERAL, false);
        NotificadorFalso n = new NotificadorFalso();

        ServicioTurnos.ResultadoUrgencia r = servicio.asignarUrgencia(rex, vet, lunesA(7, 0), n);

        assertEquals(lunesA(9, 0), r.getUrgente().getInicio());
        assertEquals(1, r.getDesplazados().size());
        assertEquals(lunesA(9, 30), comun.getInicio());   // pasó al horario siguiente
        assertEquals(1, n.avisos.size());
        assertTrue(n.avisos.get(0).startsWith("ana@x.com"));
    }

    @Test(expected = ReglaNegocioException.class)
    public void urgenciaExigeProfesionalDeGuardia() throws Exception {
        vet.setDeGuardia(false);
        servicio.asignarUrgencia(rex, vet, lunesA(7, 0), null);
    }

    @Test
    public void urgenciaNoDesplazaOtraUrgencia() throws Exception {
        servicio.asignarUrgencia(rex, vet, lunesA(7, 0), null);
        ServicioTurnos.ResultadoUrgencia r2 = servicio.asignarUrgencia(mishi, vet, lunesA(7, 0), null);
        assertEquals(lunesA(9, 30), r2.getUrgente().getInicio());
    }

    @Test
    public void listadoDeTurnosVieneOrdenadoPorFecha() throws Exception {
        servicio.asignarTurno(rex, vet, lunesA(15, 0), TipoAtencion.CONSULTA_GENERAL, false);
        servicio.asignarTurno(mishi, vet, lunesA(10, 0), TipoAtencion.CONSULTA_GENERAL, false);
        assertEquals(lunesA(10, 0), servicio.listarTurnos().get(0).getInicio());
    }

    @Test
    public void diaSemanaCubreLos7DiasDeJavaTime() {
        for (DayOfWeek d : DayOfWeek.values()) {
            assertEquals(d.getValue() - 1, DiaSemana.desdeFecha(d).ordinal());
        }
    }
}
