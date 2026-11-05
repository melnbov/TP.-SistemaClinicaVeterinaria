package persistencia;

import modelo.entidades.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.temporal.TemporalAdjusters;

import static org.junit.Assert.*;

/** Verifica que lo que se guarda en archivo vuelve igual al cargarlo (el problema original: "no se guardan las cosas"). */
public class PersistenciaTest {
    @Rule
    public TemporaryFolder carpeta = new TemporaryFolder();

    @Test
    public void sinArchivoCargarDevuelveNull() throws IOException {
        assertNull(new RepositorioArchivo(carpeta.getRoot().toPath().resolve("no-existe.dat")).cargar());
    }

    @Test
    public void guardarYCargarConservaDatosYReferencias() throws Exception {
        Path archivo = carpeta.getRoot().toPath().resolve("sub/clinica.dat"); // también crea la subcarpeta
        RepositorioArchivo repo = new RepositorioArchivo(archivo);

        DatosClinica original = DatosClinica.conDatosIniciales();
        Dueno juan = new Dueno("Juan", "Pérez", "100", "555", "j@x.com");
        juan.setPlanSalud(new PlanSalud("Plus", 15));
        original.getRegistro().registrarDueno(juan);
        Perro rex = new Perro("Rex", "Mestizo", "M", LocalDate.now().minusYears(2), 10, juan, "CHIP1", 40);
        original.getRegistro().registrarMascota(rex);
        Veterinario vet = (Veterinario) original.getPersonal().get(0);
        LocalDateTime fecha = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atTime(10, 0);
        Turno turno = original.getServicioTurnos().asignarTurno(rex, vet, fecha, TipoAtencion.CONSULTA_GENERAL, false);
        Consulta c = rex.registrarConsulta(vet, "control", LocalDate.now(), TipoAtencion.CONSULTA_GENERAL);
        c.registrarPeso(11);
        c.registrarDiagnostico("Sano", null);
        c.agregarInsumo(original.getServicioStock().buscarProducto("INS01"), 2);
        repo.guardar(original);

        assertTrue(Files.exists(archivo));
        assertFalse("No debe quedar el archivo temporal", Files.exists(archivo.resolveSibling("clinica.dat.tmp")));

        DatosClinica cargado = repo.cargar();
        assertEquals(1, cargado.getRegistro().listarDuenos().size());
        Dueno juanCargado = cargado.getRegistro().buscarDueno("100").orElseThrow(AssertionError::new);
        assertEquals("Plus", juanCargado.getPlanSalud().getNombre());
        Mascota rexCargado = cargado.getRegistro().listarMascotas().get(0);
        assertEquals("Rex", rexCargado.getNombre());
        assertSame("La mascota y el dueño deben seguir referenciándose entre sí",
                juanCargado, rexCargado.getDuenoActual());
        assertEquals(1, cargado.getServicioTurnos().listarTurnos().size());
        Turno turnoCargado = cargado.getServicioTurnos().listarTurnos().get(0);
        assertEquals(turno.getNumero(), turnoCargado.getNumero());
        assertSame(rexCargado, turnoCargado.getMascota());
        assertEquals(fecha, turnoCargado.getInicio());
        assertEquals(1, rexCargado.getHistoriaClinica().size());
        assertEquals("Sano", rexCargado.getHistoriaClinica().get(0).getDiagnostico());
        assertEquals(1, rexCargado.getHistoriaClinica().get(0).getInsumos().size());
        assertEquals(3, cargado.getPersonal().stream().filter(p -> p instanceof Veterinario).count() + 1);
        assertEquals(5, cargado.getCatalogoVacunas().size());
        assertEquals(50, cargado.getServicioStock().buscarProducto("INS01").calcularStock());
    }

    @Test
    public void despuesDeCargarLosContadoresContinuanSinRepetirNumeros() throws Exception {
        RepositorioArchivo repo = new RepositorioArchivo(carpeta.getRoot().toPath().resolve("c.dat"));
        DatosClinica d = DatosClinica.conDatosIniciales();
        Dueno juan = new Dueno("Juan", "Pérez", "100", "555", "j@x.com");
        d.getRegistro().registrarDueno(juan);
        Gato gato = new Gato("Mishi", "X", "H", LocalDate.now().minusYears(1), 4, juan, EstiloVida.INTERIOR);
        d.getRegistro().registrarMascota(gato);
        Veterinario vet = (Veterinario) d.getPersonal().get(0);
        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        Turno t = d.getServicioTurnos().asignarTurno(gato, vet, lunes.atTime(10, 0), TipoAtencion.CONSULTA_GENERAL, false);
        Consulta c = gato.registrarConsulta(vet, "control");
        repo.guardar(d);

        // Simula un proceso nuevo: los contadores estáticos vuelven a su valor inicial.
        DatosClinica cargado = repo.cargar();
        cargado.restaurarContadores();
        Mascota gatoCargado = cargado.getRegistro().listarMascotas().get(0);
        Turno nuevo = cargado.getServicioTurnos()
                .asignarTurno(gatoCargado, vet, lunes.atTime(11, 0), TipoAtencion.CONSULTA_GENERAL, false);
        Consulta nueva = gatoCargado.registrarConsulta(vet, "otra");
        assertTrue("N° de turno repetido", nuevo.getNumero() > t.getNumero());
        assertTrue("N° de consulta repetido", nueva.getNumero() > c.getNumero());
    }

    @Test
    public void archivoCorruptoSeInformaComoIOException() throws IOException {
        Path archivo = carpeta.newFile("roto.dat").toPath();
        Files.write(archivo, new byte[]{1, 2, 3, 4});
        try {
            new RepositorioArchivo(archivo).cargar();
            fail("Debía fallar");
        } catch (IOException esperado) { /* ok */ }
    }
}
