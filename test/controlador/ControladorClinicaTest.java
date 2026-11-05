package controlador;

import modelo.entidades.*;
import modelo.servicios.EmailNotificador;
import modelo.excepciones.ReglaNegocioException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import persistencia.RepositorioArchivo;

import java.nio.file.Path;
import java.time.*;
import java.time.temporal.TemporalAdjusters;

import static org.junit.Assert.*;

/** Casos de uso completos a través del controlador (incluye reabrir la aplicación para comprobar el guardado). */
public class ControladorClinicaTest {
    @Rule
    public TemporaryFolder carpeta = new TemporaryFolder();

    private Path archivo;
    private ControladorClinica ctrl;
    private Dueno juan;
    private Perro rex;
    private Veterinario ana;
    private LocalDate lunes;

    @Before
    public void preparar() throws Exception {
        archivo = carpeta.getRoot().toPath().resolve("clinica.dat");
        ctrl = new ControladorClinica(new RepositorioArchivo(archivo), new EmailNotificador());
        juan = ctrl.registrarDueno("Juan", "Pérez", "100", "555", "j@x.com", new PlanSalud("Plus", 15));
        rex = ctrl.nuevoPerro("Rex", "Mestizo", "M", LocalDate.now().minusYears(2), 10, juan, "CHIP1", 40);
        ctrl.registrarMascota(rex);
        ana = ctrl.listarVeterinarios().get(0);
        lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    private Consulta consultaConDiagnostico() throws Exception {
        Turno t = ctrl.asignarTurno(rex, ana, lunes.atTime(10, 0), TipoAtencion.CONSULTA_GENERAL, false);
        Consulta c = ctrl.atenderTurno(t.getNumero(), "control");
        ctrl.registrarPeso(c, 11);
        ctrl.registrarDiagnostico(c, "Otitis", null);
        return c;
    }

    private int stock(String codigo) {
        for (Producto p : ctrl.listarProductos()) if (p.getCodigo().equals(codigo)) return p.calcularStock();
        throw new AssertionError("producto inexistente");
    }

    @Test
    public void elStockNoSeDescuentaAlIndicarElTratamientoSinoAlConfirmar() throws Exception {
        Consulta c = consultaConDiagnostico();
        int antes = stock("MED01");
        ctrl.indicarTratamiento(c, "MED01", "1 comp", "c/12h", "7 días");
        ctrl.agregarInsumo(c, "INS01", 3);
        assertEquals(antes, stock("MED01"));          // todavía no se descontó
        assertEquals(50, stock("INS01"));

        Factura f = ctrl.confirmarConsulta(c);

        assertEquals(antes - 1, stock("MED01"));
        assertEquals(47, stock("INS01"));
        assertTrue(c.estaCerrada());
        assertEquals(3, f.getLineas().size());        // consulta + medicamento + insumo
        assertEquals(EstadoFactura.EMITIDA, f.getEstado());
    }

    @Test
    public void noSePuedeIndicarOAplicarUnInsumoSinStock() throws Exception {
        Consulta c = consultaConDiagnostico();
        try {
            ctrl.agregarInsumo(c, "INS01", 51);
            fail("Debía rechazar por falta de stock");
        } catch (ReglaNegocioException esperado) { /* ok */ }
        assertEquals(50, stock("INS01"));
    }

    @Test
    public void unaConfirmacionFallidaNoDejaDescuentosAMedias() throws Exception {
        Consulta c = consultaConDiagnostico();
        ctrl.agregarInsumo(c, "INS01", 40);
        ctrl.agregarInsumo(c, "INS01", 40); // cada uno pasa solo, juntos superan las 50 unidades
        try {
            ctrl.confirmarConsulta(c);
            fail("Debía rechazar");
        } catch (ReglaNegocioException esperado) { /* ok */ }
        assertFalse(c.estaCerrada());
        assertEquals(50, stock("INS01"));
    }

    @Test
    public void cobrarConPlanYEfectivoAplicaElMayorDescuento() throws Exception {
        Consulta c = consultaConDiagnostico();
        Factura f = ctrl.confirmarConsulta(c);
        double subtotal = f.calcularSubtotal();
        ctrl.pagarFactura(f.getNumero(), new Efectivo());
        assertEquals(EstadoFactura.PAGADA, f.getEstado());
        assertEquals(subtotal * 0.85, f.getPago().getImporte(), 0.01);
    }

    @Test(expected = ReglaNegocioException.class)
    public void unaFacturaPagadaNoSePuedePagarDeNuevo() throws Exception {
        Factura f = ctrl.confirmarConsulta(consultaConDiagnostico());
        ctrl.pagarFactura(f.getNumero(), new Debito());
        ctrl.pagarFactura(f.getNumero(), new Debito());
    }

    @Test
    public void anularUnaFacturaGeneraNotaDeCredito() throws Exception {
        Factura f = ctrl.confirmarConsulta(consultaConDiagnostico());
        NotaCredito nc = ctrl.anularFactura(f.getNumero(), "Error");
        assertEquals(EstadoFactura.ANULADA, f.getEstado());
        assertEquals(f.calcularSubtotal(), nc.getImporte(), 0.01);
    }

    @Test
    public void todoSeConservaAlReabrirLaAplicacion() throws Exception {
        Consulta c = consultaConDiagnostico();
        ctrl.indicarTratamiento(c, "MED01", "1", "c/12h", "5 días");
        Factura f = ctrl.confirmarConsulta(c);
        ctrl.pagarFactura(f.getNumero(), new Debito());
        ctrl.registrarMedicamento("MED99", "Nuevo", 2, "pa", "gotas");

        ControladorClinica reabierto = new ControladorClinica(new RepositorioArchivo(archivo), new EmailNotificador());

        assertTrue(reabierto.getMensajeInicio().startsWith("Datos cargados"));
        assertEquals(1, reabierto.listarDuenos().size());
        assertEquals("Rex", reabierto.listarMascotas().get(0).getNombre());
        assertEquals(1, reabierto.listarTurnos().size());
        assertEquals(EstadoTurno.ATENDIDO, reabierto.listarTurnos().get(0).getEstado());
        assertEquals(EstadoFactura.PAGADA, reabierto.listarFacturas().get(0).getEstado());
        assertEquals(1, reabierto.listarMascotas().get(0).getHistoriaClinica().size());
        assertEquals(30 - 1, reabierto.listarProductos().stream()
                .filter(p -> p.getCodigo().equals("MED01")).findFirst().get().calcularStock());
        assertEquals(4, reabierto.listarProductos().size());        // 3 iniciales + el medicamento nuevo
    }

    @Test
    public void datosInvalidosSeInformanComoReglaDeNegocio() {
        try {
            ctrl.registrarDueno("", "X", "999", "1", "a@b.com", null);
            fail();
        } catch (ReglaNegocioException esperado) { /* ok */ }
        try {
            ctrl.registrarDueno("A", "B", "100", "1", "a@b.com", null); // documento repetido
            fail();
        } catch (ReglaNegocioException esperado) { /* ok */ }
    }
}
