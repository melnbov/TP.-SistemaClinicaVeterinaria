package modelo;

import modelo.entidades.*;
import modelo.excepciones.ReglaNegocioException;
import modelo.servicios.RegistroDuenosMascotas;
import modelo.servicios.ServicioStock;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.*;

/** Pruebas de las reglas de negocio de mascotas, consultas, stock, vacunas y facturación. */
public class ClinicaModeloTest {
    private Dueno dueno;
    private Perro rex;
    private Veterinario vet;

    @Before
    public void preparar() {
        dueno = new Dueno("Juan", "Pérez", "100", "555", "j@x.com");
        rex = new Perro("Rex", "Mestizo", "M", LocalDate.now().minusYears(2), 10, dueno, "CHIP1", 40);
        vet = new Veterinario("Ana", "Gómez", "1", "1", "M-1", "General");
    }

    // ---------------- stock
    private Medicamento medicamento(int minimo) {
        return new Medicamento("M1", "Amoxicilina", minimo, "Amoxicilina", "Comp");
    }

    @Test
    public void alertaDeStockAlLlegarAlMinimo() {
        Medicamento m = medicamento(5);
        m.agregarLote(new LoteProducto("L1", LocalDate.now().plusMonths(6), 6, 100));
        assertFalse(m.tieneStockBajo());          // 6 > 5
        m.getLotes().get(0).descontar(1);
        assertTrue(m.tieneStockBajo());           // 5 == mínimo: "al llegar a ese valor" ya alerta
    }

    @Test
    public void consumePrimeroElLoteQueVenceAntes() {
        ServicioStock s = new ServicioStock();
        Medicamento m = medicamento(1);
        LoteProducto tarde = new LoteProducto("TARDE", LocalDate.now().plusMonths(10), 10, 100);
        LoteProducto temprano = new LoteProducto("TEMPRANO", LocalDate.now().plusMonths(1), 4, 100);
        m.agregarLote(tarde);
        m.agregarLote(temprano);
        s.registrarProducto(m);
        s.descontarPorVencimiento("M1", 6);
        assertEquals(0, temprano.getCantidadDisponible());
        assertEquals(8, tarde.getCantidadDisponible());
    }

    @Test
    public void unLoteVencidoNoPuedeUsarse() {
        ServicioStock s = new ServicioStock();
        Medicamento m = medicamento(1);
        m.agregarLote(new LoteProducto("VIEJO", LocalDate.now().minusDays(1), 50, 100));
        s.registrarProducto(m);
        assertEquals(0, m.calcularStock());
        try {
            s.descontarPorVencimiento("M1", 1);
            fail("No debe usar lotes vencidos");
        } catch (IllegalStateException esperado) { /* ok */ }
    }

    @Test(expected = IllegalStateException.class)
    public void noSePuedeDescontarSinStock() {
        ServicioStock s = new ServicioStock();
        Medicamento m = medicamento(1);
        m.agregarLote(new LoteProducto("L", LocalDate.now().plusMonths(1), 2, 100));
        s.registrarProducto(m);
        s.descontarPorVencimiento("M1", 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void noSePuedeRepetirElCodigoDeProducto() {
        ServicioStock s = new ServicioStock();
        s.registrarProducto(medicamento(1));
        s.registrarProducto(medicamento(2));
    }

    // ---------------- consultas
    @Test
    public void noSeIndicaMedicamentoSinPesoDelDia() throws Exception {
        Consulta c = rex.registrarConsulta(vet, "control");
        try {
            c.indicarTratamiento(medicamento(1), "1", "c/8h", "5 días");
            fail("Debe exigir el peso");
        } catch (ReglaNegocioException esperado) {
            assertTrue(esperado.getMessage().contains("peso"));
        }
        c.registrarPeso(11);
        assertNotNull(c.indicarTratamiento(medicamento(1), "1", "c/8h", "5 días"));
        assertEquals(11, rex.getPesoActual(), 0.001);
    }

    @Test
    public void consultaCerradaSoloAdmiteNotasRectificatorias() throws Exception {
        Consulta c = rex.registrarConsulta(vet, "control");
        c.registrarDiagnostico("Sano", null);
        c.cerrar();
        try {
            c.registrarDiagnostico("Otro", null);
            fail("No debe modificar una consulta cerrada");
        } catch (ReglaNegocioException esperado) { /* ok */ }
        c.agregarNotaRectificatoria("Corrección de diagnóstico", vet);
        assertEquals(1, c.getNotasRectificatorias().size());
        assertEquals("Sano", c.getDiagnostico());
    }

    @Test(expected = ReglaNegocioException.class)
    public void notaRectificatoriaSoloEnConsultaCerrada() throws Exception {
        rex.registrarConsulta(vet, "control").agregarNotaRectificatoria("x", vet);
    }

    @Test(expected = ReglaNegocioException.class)
    public void noSeCierraSinDiagnostico() throws Exception {
        rex.registrarConsulta(vet, "control").cerrar();
    }

    @Test(expected = IllegalArgumentException.class)
    public void soloUnVeterinarioPuedeRegistrarConsultas() {
        rex.registrarConsulta(null, "control"); // recepción no tiene perfil de veterinario
    }

    // ---------------- vacunas
    private Vacuna antirrabica() {
        return new Vacuna("V1", "Antirrábica", EnumSet.of(Especie.PERRO, Especie.GATO), 12, 365);
    }

    @Test
    public void vacunaCalculaElProximoRefuerzoSegunElIntervalo() throws Exception {
        Consulta c = rex.registrarConsulta(vet, "vacuna");
        VacunaAplicada va = c.aplicarVacuna(antirrabica(), "LOTE-1");
        assertEquals(LocalDate.now().plusDays(365), va.calcularProximoRefuerzo());
    }

    @Test
    public void noSeVacunaAUnAnimalMenorALaEdadMinima() throws Exception {
        Perro cachorro = new Perro("Bebé", "Mestizo", "M", LocalDate.now().minusWeeks(5), 2, dueno, "CHIP2", 20);
        try {
            cachorro.registrarConsulta(vet, "vacuna").aplicarVacuna(antirrabica(), "L");
            fail("Debía rechazar por edad");
        } catch (ReglaNegocioException esperado) { /* ok */ }
    }

    @Test(expected = ReglaNegocioException.class)
    public void noSeAplicaUnaVacunaDeOtraEspecie() throws Exception {
        Vacuna soloGatos = new Vacuna("V2", "Triple felina", EnumSet.of(Especie.GATO), 8, 365);
        rex.registrarConsulta(vet, "vacuna").aplicarVacuna(soloGatos, "L");
    }

    /** Aplica la antirrábica en una consulta fechada "dias" atrás (VacunaAplicada solo se crea desde Consulta). */
    private VacunaAplicada aplicadaHace(int dias) throws Exception {
        return rex.registrarConsulta(vet, "vacuna", LocalDate.now().minusDays(dias)).aplicarVacuna(antirrabica(), "L");
    }

    @Test
    public void recordatorioSeEmite7DiasAntesDelRefuerzo() throws Exception {
        assertTrue(aplicadaHace(365 - 7).debeRecordarse(LocalDate.now()));   // refuerzo en 7 días
        assertFalse(aplicadaHace(365 - 8).debeRecordarse(LocalDate.now()));  // refuerzo en 8 días
    }

    @Test
    public void vacunaSeConsideraVencidaPasados30DiasDelRefuerzo() throws Exception {
        assertTrue(aplicadaHace(365 + 31).estaVencida(LocalDate.now()));
        assertFalse(aplicadaHace(365 + 30).estaVencida(LocalDate.now()));
    }

    // ---------------- dueños y mascotas
    @Test
    public void transferirMascotaConservaSuHistoriaClinica() throws Exception {
        RegistroDuenosMascotas reg = new RegistroDuenosMascotas();
        Dueno nuevo = new Dueno("Ana", "López", "200", "444", "a@x.com");
        reg.registrarDueno(dueno);
        reg.registrarDueno(nuevo);
        reg.registrarMascota(rex);
        rex.registrarConsulta(vet, "control");
        reg.transferirMascota(rex, nuevo, LocalDate.now());
        assertEquals(nuevo, rex.getDuenoActual());
        assertEquals(1, rex.getHistoriaClinica().size());
        assertEquals(2, rex.getHistorialDuenos().size());
        assertTrue(reg.listarMascotasDe(dueno).isEmpty());
        assertEquals(1, reg.listarMascotasDe(nuevo).size());
    }

    @Test(expected = ReglaNegocioException.class)
    public void noSePuedeRegistrarDosVecesElMismoDocumento() throws Exception {
        RegistroDuenosMascotas reg = new RegistroDuenosMascotas();
        reg.registrarDueno(dueno);
        reg.registrarDueno(new Dueno("Otro", "Distinto", "100", "1", "o@x.com"));
    }

    @Test(expected = ReglaNegocioException.class)
    public void noSeRegistraUnPerroConChipRepetido() throws Exception {
        RegistroDuenosMascotas reg = new RegistroDuenosMascotas();
        reg.registrarDueno(dueno);
        reg.registrarMascota(rex);
        reg.registrarMascota(new Perro("Otro", "X", "M", LocalDate.now().minusYears(1), 5, dueno, "chip1", 30));
    }

    @Test
    public void ordenNaturalDeDuenosYOrdenPersonalizadoDeMascotas() throws Exception {
        RegistroDuenosMascotas reg = new RegistroDuenosMascotas();
        Dueno zeta = new Dueno("Zoe", "Zárate", "300", "1", "z@x.com");
        reg.registrarDueno(zeta);
        reg.registrarDueno(dueno);
        List<Dueno> duenos = reg.listarDuenos();
        assertEquals("Pérez", duenos.get(0).getApellido());   // Comparable: por apellido
        reg.registrarMascota(new Gato("Zeus", "X", "M", LocalDate.now().minusYears(1), 4, zeta, EstiloVida.EXTERIOR));
        reg.registrarMascota(rex);
        assertEquals("Rex", reg.listarMascotas().get(0).getNombre()); // Comparator: por nombre
    }

    // ---------------- facturación
    private Factura factura() {
        Factura f = new Factura(1, LocalDate.now());
        f.agregarLinea(new LineaFactura("Consulta", 1, 10000));
        return f;
    }

    @Test
    public void efectivoDa10PorCientoDeDescuento() {
        assertEquals(9000, factura().calcularTotal(null, new Efectivo()), 0.001);
    }

    @Test
    public void debitoNoTieneRecargoYCreditoSuma8PorCiento() {
        assertEquals(10000, factura().calcularTotal(null, new Debito()), 0.001);
        assertEquals(10800, factura().calcularTotal(null, new Credito(3)), 0.001);
    }

    @Test
    public void planDeSaludYEfectivoNoSeAcumulanSeAplicaElMayor() {
        PlanSalud plan15 = new PlanSalud("Plus", 15);
        assertEquals(8500, factura().calcularTotal(plan15, new Efectivo()), 0.001);  // 15 > 10
        PlanSalud plan5 = new PlanSalud("Basico", 5);
        assertEquals(9000, factura().calcularTotal(plan5, new Efectivo()), 0.001);   // 10 > 5
    }

    @Test(expected = IllegalStateException.class)
    public void facturaPagadaNoSeModifica() {
        Factura f = factura();
        f.registrarPago(new Pago(LocalDate.now(), 10000, new Debito()));
        f.agregarLinea(new LineaFactura("Extra", 1, 5));
    }

    @Test
    public void facturaEmitidaSeAnulaConNotaDeCredito() {
        Factura f = factura();
        NotaCredito nc = f.anular(1, "Error de carga");
        assertEquals(EstadoFactura.ANULADA, f.getEstado());
        assertEquals(10000, nc.getImporte(), 0.001);
        assertSame(nc, f.getNotaCredito());
    }
}
