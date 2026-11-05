package persistencia;

import modelo.entidades.*;
import modelo.servicios.RegistroDuenosMascotas;
import modelo.servicios.ServicioStock;
import modelo.servicios.ServicioTurnos;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Todo el estado de la clínica en un único objeto. Se serializa completo, de modo que
 * las referencias entre objetos (turnos -> mascotas -> dueños) se conservan al volver a cargar.
 */
public class DatosClinica implements Serializable {
    private static final long serialVersionUID = 1L;

    private final RegistroDuenosMascotas registro = new RegistroDuenosMascotas();
    private final ServicioTurnos servicioTurnos = new ServicioTurnos();
    private final ServicioStock servicioStock = new ServicioStock();
    private final List<Personal> personal = new ArrayList<>();
    private final List<Vacuna> catalogoVacunas = new ArrayList<>();
    private final List<Factura> facturas = new ArrayList<>();
    private int proximaFactura = 1;
    private int proximaNotaCredito = 1;

    public RegistroDuenosMascotas getRegistro() { return registro; }
    public ServicioTurnos getServicioTurnos() { return servicioTurnos; }
    public ServicioStock getServicioStock() { return servicioStock; }
    public List<Personal> getPersonal() { return personal; }
    public List<Vacuna> getCatalogoVacunas() { return catalogoVacunas; }
    public List<Factura> getFacturas() { return facturas; }
    public int tomarNumeroFactura() { return proximaFactura++; }
    public int tomarNumeroNotaCredito() { return proximaNotaCredito++; }

    /**
     * Los contadores de Turno y Consulta son estáticos y no se serializan:
     * al cargar hay que volver a dejarlos después del último número usado.
     */
    public void restaurarContadores() {
        Turno.actualizarSecuencia(servicioTurnos.getUltimoNumero());
        int ultimaConsulta = 0;
        for (Mascota m : registro.listarMascotas()) {
            for (Consulta c : m.getHistoriaClinica()) ultimaConsulta = Math.max(ultimaConsulta, c.getNumero());
        }
        Consulta.actualizarSecuencia(ultimaConsulta);
    }

    /** Datos de partida para la primera ejecución (así el menú se puede probar enseguida). */
    public static DatosClinica conDatosIniciales() {
        DatosClinica d = new DatosClinica();

        Veterinario ana = new Veterinario("Ana", "Gómez", "30111222", "1155550001", "M-1001", "Clínica general");
        ana.setDeGuardia(true);
        Veterinario luis = new Veterinario("Luis", "Pérez", "28999111", "1155550002", "M-1002", "Cirugía");
        for (DiaSemana dia : EnumSet.range(DiaSemana.LUNES, DiaSemana.SABADO)) {
            ana.agregarHorario(new HorarioAtencion(dia, LocalTime.of(9, 0), LocalTime.of(19, 0)));
        }
        for (DiaSemana dia : EnumSet.range(DiaSemana.LUNES, DiaSemana.VIERNES)) {
            luis.agregarHorario(new HorarioAtencion(dia, LocalTime.of(9, 0), LocalTime.of(19, 0)));
        }
        d.personal.add(ana);
        d.personal.add(luis);
        d.personal.add(new Recepcionista("Marta", "Díaz", "27555666", "1155550003"));
        d.personal.add(new Administrador("Carlos", "Ruiz", "25444333", "1155550004"));

        // Calendario de vacunación por especie (valores de referencia, supuesto del grupo).
        d.catalogoVacunas.add(new Vacuna("V01", "Antirrábica", EnumSet.of(Especie.PERRO, Especie.GATO), 12, 365));
        d.catalogoVacunas.add(new Vacuna("V02", "Quíntuple canina", EnumSet.of(Especie.PERRO), 6, 365));
        d.catalogoVacunas.add(new Vacuna("V03", "Triple felina", EnumSet.of(Especie.GATO), 8, 365));
        d.catalogoVacunas.add(new Vacuna("V04", "Mixomatosis", EnumSet.of(Especie.CONEJO), 10, 180));
        d.catalogoVacunas.add(new Vacuna("V05", "Newcastle", EnumSet.of(Especie.AVE), 8, 180));

        LocalDate vence = LocalDate.now().plusYears(1);
        Medicamento amoxicilina = new Medicamento("MED01", "Amoxicilina", 5, "Amoxicilina", "Comprimidos 250 mg");
        amoxicilina.agregarLote(new LoteProducto("L-AMX-01", vence, 30, 800));
        Medicamento meloxicam = new Medicamento("MED02", "Meloxicam", 5, "Meloxicam", "Suspensión 10 ml");
        meloxicam.agregarLote(new LoteProducto("L-MLX-01", vence, 20, 1200));
        Insumo gasas = new Insumo("INS01", "Gasas estériles", 10, "paquete");
        gasas.agregarLote(new LoteProducto("L-GAS-01", vence, 50, 300));
        d.servicioStock.registrarProducto(amoxicilina);
        d.servicioStock.registrarProducto(meloxicam);
        d.servicioStock.registrarProducto(gasas);
        return d;
    }
}
