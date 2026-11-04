package controlador;

import modelo.entidades.*;
import modelo.excepciones.ReglaNegocioException;
import modelo.servicios.*;
import persistencia.DatosClinica;
import persistencia.RepositorioArchivo;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Controlador (GRASP): recibe las acciones de la interfaz, coordina los servicios del modelo
 * y guarda el estado en archivo después de cada operación que lo modifica.
 */
public class ControladorClinica {
    private final RepositorioArchivo repositorio;
    private final Notificador notificador;
    private final ServicioRecordatorios servicioRecordatorios;
    private DatosClinica datos;
    private String mensajeInicio = "";

    /** El {@link Notificador} se inyecta desde afuera (lo elige Main): el controlador no conoce Email ni SMS. */
    public ControladorClinica(RepositorioArchivo repositorio, Notificador notificador) {
        if (notificador == null) throw new IllegalArgumentException("Notificador obligatorio");
        this.repositorio = repositorio;
        this.notificador = notificador;
        this.servicioRecordatorios = new ServicioRecordatorios(notificador);
        cargarDatos();
    }

    /** Carga el archivo si existe; si no existe (o está dañado) arranca con los datos iniciales. */
    private void cargarDatos() {
        try {
            datos = repositorio.cargar();
            if (datos != null) {
                datos.restaurarContadores();
                mensajeInicio = "Datos cargados desde " + repositorio.getArchivo();
                return;
            }
            mensajeInicio = "Primera ejecución: se crearon datos iniciales.";
        } catch (IOException | RuntimeException ex) {
            mensajeInicio = "No se pudo leer " + repositorio.getArchivo() + " (" + ex.getMessage()
                    + "). Se comienza con datos iniciales.";
        }
        datos = DatosClinica.conDatosIniciales();
        guardar();
    }

    public String getMensajeInicio() { return mensajeInicio; }

    private void guardar() {
        try {
            repositorio.guardar(datos);
        } catch (IOException ex) {
            System.out.println("¡ATENCIÓN! No se pudieron guardar los datos: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------- dueños y mascotas

    public Dueno registrarDueno(String nombre, String apellido, String documento,
                                String telefono, String email, PlanSalud plan) throws ReglaNegocioException {
        Dueno d = crear(() -> new Dueno(nombre, apellido, documento, telefono, email));
        d.setPlanSalud(plan);
        datos.getRegistro().registrarDueno(d);
        guardar();
        return d;
    }

    public Dueno buscarDueno(String documento) {
        return datos.getRegistro().buscarDueno(documento).orElse(null);
    }

    public List<Dueno> listarDuenos() { return datos.getRegistro().listarDuenos(); }
    public List<Mascota> listarMascotas() { return datos.getRegistro().listarMascotas(); }
    public List<Mascota> listarMascotasDe(Dueno dueno) { return datos.getRegistro().listarMascotasDe(dueno); }

    public Mascota registrarMascota(Mascota mascota) throws ReglaNegocioException {
        datos.getRegistro().registrarMascota(mascota);
        guardar();
        return mascota;
    }

    public Perro nuevoPerro(String nombre, String raza, String sexo, LocalDate nacimiento, double peso,
                            Dueno dueno, String chip, double tamanio) throws ReglaNegocioException {
        return crear(() -> new Perro(nombre, raza, sexo, nacimiento, peso, dueno, chip, tamanio));
    }
    public Gato nuevoGato(String nombre, String raza, String sexo, LocalDate nacimiento, double peso,
                          Dueno dueno, EstiloVida vida) throws ReglaNegocioException {
        return crear(() -> new Gato(nombre, raza, sexo, nacimiento, peso, dueno, vida));
    }
    public Ave nuevaAve(String nombre, String raza, String sexo, LocalDate nacimiento, double peso,
                        Dueno dueno, boolean vuela) throws ReglaNegocioException {
        return crear(() -> new Ave(nombre, raza, sexo, nacimiento, peso, dueno, vuela));
    }
    public Conejo nuevoConejo(String nombre, String raza, String sexo, LocalDate nacimiento, double peso,
                              Dueno dueno) throws ReglaNegocioException {
        return crear(() -> new Conejo(nombre, raza, sexo, nacimiento, peso, dueno));
    }

    public void transferirMascota(Mascota mascota, String documentoNuevoDueno) throws ReglaNegocioException {
        Dueno nuevo = buscarDueno(documentoNuevoDueno);
        if (nuevo == null) throw new ReglaNegocioException("No existe un dueño con ese documento");
        datos.getRegistro().transferirMascota(mascota, nuevo, LocalDate.now());
        guardar();
    }

    // ---------------------------------------------------------------- personal

    public Veterinario registrarVeterinario(String nombre, String apellido, String documento, String telefono,
                                            String matricula, String especialidad, boolean deGuardia,
                                            int diasDesdeLunes, LocalTime desde, LocalTime hasta)
            throws ReglaNegocioException {
        for (Veterinario v : listarVeterinarios()) {
            if (v.getMatricula().equalsIgnoreCase(matricula == null ? "" : matricula.trim()))
                throw new ReglaNegocioException("Ya existe un veterinario con esa matrícula");
        }
        Veterinario v = crear(() -> {
            Veterinario nuevo = new Veterinario(nombre, apellido, documento, telefono,
                    matricula == null ? null : matricula.trim(), especialidad);
            nuevo.setDeGuardia(deGuardia);
            DiaSemana[] dias = DiaSemana.values();
            for (int i = 0; i < Math.min(Math.max(diasDesdeLunes, 1), 7); i++) {
                nuevo.agregarHorario(new HorarioAtencion(dias[i], desde, hasta));
            }
            return nuevo;
        });
        datos.getPersonal().add(v);
        guardar();
        return v;
    }
    /** Busca a un integrante del personal por documento (para el ingreso al sistema). */
    public Personal buscarPersonal(String documento) {
        for (Personal p : datos.getPersonal()) if (p.getDocumento().equals(documento)) return p;
        return null;
    }
    public List<Veterinario> listarVeterinarios() {
        List<Veterinario> r = new ArrayList<>();
        for (Personal p : datos.getPersonal()) if (p instanceof Veterinario) r.add((Veterinario) p);
        return r;
    }

    // ---------------------------------------------------------------- turnos

    public Turno asignarTurno(Mascota mascota, Veterinario veterinario, LocalDateTime fecha,
                              TipoAtencion tipo, boolean senaAbonada) throws ReglaNegocioException {
        Turno t = datos.getServicioTurnos().asignarTurno(mascota, veterinario, fecha, tipo, false, senaAbonada);
        guardar();
        return t;
    }

    public ServicioTurnos.ResultadoUrgencia asignarUrgencia(Mascota mascota, Veterinario guardia)
            throws ReglaNegocioException {
        ServicioTurnos.ResultadoUrgencia r =
                datos.getServicioTurnos().asignarUrgencia(mascota, guardia, LocalDateTime.now(), notificador);
        guardar();
        return r;
    }

    public void reprogramarTurno(int numero, LocalDateTime fecha) throws ReglaNegocioException {
        datos.getServicioTurnos().reprogramarTurno(numero, fecha);
        guardar();
    }

    /** @return el cargo por cancelación tardía (0 si no corresponde) */
    public double cancelarTurno(int numero) throws ReglaNegocioException {
        double cargo = datos.getServicioTurnos().cancelarTurno(numero, LocalDateTime.now());
        guardar();
        return cargo;
    }

    public void registrarAusencia(int numero) throws ReglaNegocioException {
        datos.getServicioTurnos().registrarAusencia(numero);
        guardar();
    }
    /** Próximos días (a partir de hoy) en los que el veterinario atiende. */
    public List<LocalDate> diasDeAtencion(Veterinario v, int cantidadDias) {
        List<LocalDate> r = new ArrayList<>();
        LocalDate dia = LocalDate.now();
        for (int i = 0; i < cantidadDias; i++, dia = dia.plusDays(1)) {
            DiaSemana ds = DiaSemana.desdeFecha(dia.getDayOfWeek());
            for (HorarioAtencion h : v.getHorarios()) {
                if (h.getDiaSemana() == ds) { r.add(dia); break; }
            }
        }
        return r;
    }

    /** Horarios libres de un veterinario en un día para una atención (grilla de 30', o 15' si la atención dura 15'). */
    public List<LocalDateTime> horariosLibres(Veterinario v, LocalDate dia, TipoAtencion tipo, Turno excluir) {
        List<LocalDateTime> r = new ArrayList<>();
        int paso = Math.min(tipo.getDuracionMinutos(), 30);
        LocalDateTime ahora = LocalDateTime.now();
        for (LocalDateTime t = dia.atStartOfDay(); t.toLocalDate().equals(dia); t = t.plusMinutes(paso)) {
            if (!t.isAfter(ahora) || !v.estaDisponible(t, tipo.getDuracionMinutos())) continue;
            LocalDateTime fin = t.plusMinutes(tipo.getDuracionMinutos());
            boolean ocupado = false;
            for (Turno e : turnosReservados(v, dia)) {
                if (e != excluir && t.isBefore(e.getFin()) && e.getInicio().isBefore(fin)) { ocupado = true; break; }
            }
            if (!ocupado) r.add(t);
        }
        return r;
    }

    /** Turnos programados (reservados) de un veterinario en un día, ordenados por hora. */
    public List<Turno> turnosReservados(Veterinario v, LocalDate dia) {
        List<Turno> r = new ArrayList<>();
        for (Turno t : listarTurnosDe(v)) {
            if (t.getEstado() == EstadoTurno.PROGRAMADO && t.getInicio().toLocalDate().equals(dia)) r.add(t);
        }
        return r;
    }
    public boolean requiereSena(Dueno dueno) { return datos.getServicioTurnos().requiereSena(dueno); }
    public List<Turno> listarTurnos() { return datos.getServicioTurnos().listarTurnos(); }
    public List<Turno> listarTurnosDe(Veterinario v) { return datos.getServicioTurnos().listarTurnosPorVeterinario(v); }

    // ---------------------------------------------------------------- atención clínica

    /** Marca el turno como atendido y abre la consulta en la historia clínica de la mascota. */
    public Consulta atenderTurno(int numeroTurno, String motivo) throws ReglaNegocioException {
        Turno turno = datos.getServicioTurnos().buscarPorNumero(numeroTurno);
        if (turno.getEstado() != EstadoTurno.PROGRAMADO)
            throw new ReglaNegocioException("El turno no se encuentra programado");
        Consulta c = crear(() -> turno.getMascota().registrarConsulta(
                turno.getVeterinario(), motivo, LocalDate.now(), turno.getAtencion()));
        datos.getServicioTurnos().registrarAtencion(numeroTurno);
        guardar();
        return c;
    }

    public List<Consulta> consultasAbiertas() {
        List<Consulta> r = new ArrayList<>();
        for (Mascota m : datos.getRegistro().listarMascotas())
            for (Consulta c : m.getHistoriaClinica()) if (!c.estaCerrada()) r.add(c);
        return r;
    }

    public void registrarPeso(Consulta c, double peso) throws ReglaNegocioException {
        c.registrarPeso(peso);
        guardar();
    }

    public void registrarDiagnostico(Consulta c, String diagnostico, String observaciones) throws ReglaNegocioException {
        c.registrarDiagnostico(diagnostico, observaciones);
        guardar();
    }

    public Tratamiento indicarTratamiento(Consulta c, String codigoMedicamento, String dosis,
                                          String frecuencia, String duracion) throws ReglaNegocioException {
        Producto p = datos.getServicioStock().buscarProducto(codigoMedicamento);
        if (!(p instanceof Medicamento)) throw new ReglaNegocioException("No existe un medicamento con ese código");
        if (!datos.getServicioStock().hayStock(codigoMedicamento, 1))
            throw new ReglaNegocioException("No hay stock de " + p.getNombre());
        Tratamiento t = c.indicarTratamiento((Medicamento) p, dosis, frecuencia, duracion);
        guardar();
        return t;
    }

    public VacunaAplicada aplicarVacuna(Consulta c, String codigoVacuna, String lote) throws ReglaNegocioException {
        for (Vacuna v : datos.getCatalogoVacunas()) {
            if (v.getCodigo().equalsIgnoreCase(codigoVacuna == null ? "" : codigoVacuna.trim())) {
                VacunaAplicada va = c.aplicarVacuna(v, lote);
                guardar();
                return va;
            }
        }
        throw new ReglaNegocioException("No existe la vacuna " + codigoVacuna + " en el catálogo");
    }

    public void agregarInsumo(Consulta c, String codigoProducto, int cantidad) throws ReglaNegocioException {
        Producto p = datos.getServicioStock().buscarProducto(codigoProducto == null ? "" : codigoProducto);
        if (p == null) throw new ReglaNegocioException("No existe un producto con ese código");
        if (!datos.getServicioStock().hayStock(p.getCodigo(), cantidad))
            throw new ReglaNegocioException("Stock insuficiente de " + p.getNombre()
                    + " (disponible: " + p.calcularStock() + ")");
        c.agregarInsumo(p, cantidad);
        guardar();
    }

    /**
     * Confirma la consulta: la cierra, descuenta el stock (recién ahora, no al indicar el tratamiento)
     * consumiendo primero el lote que vence antes, y emite la factura con la consulta y los insumos.
     */
    public Factura confirmarConsulta(Consulta c) throws ReglaNegocioException {
        if (c.estaCerrada()) throw new ReglaNegocioException("La consulta ya está cerrada");
        if (c.getDiagnostico() == null) throw new ReglaNegocioException("No se puede cerrar la consulta sin un diagnóstico");

        // 1) Cantidades necesarias por producto y precios (antes de descontar, porque el precio sale del lote).
        Map<String, Integer> necesarios = new LinkedHashMap<>();
        List<LineaFactura> lineas = new ArrayList<>();
        lineas.add(new LineaFactura("Consulta - " + c.getTipoAtencion(), 1, c.getTipoAtencion().getPrecio()));
        for (Tratamiento t : c.getTratamientos()) {
            necesarios.merge(t.getMedicamento().getCodigo(), 1, Integer::sum);
            lineas.add(new LineaFactura(t.getMedicamento().getNombre(), 1, t.getMedicamento().getPrecioVenta()));
        }
        for (InsumoUtilizado i : c.getInsumos()) {
            necesarios.merge(i.getProducto().getCodigo(), i.getCantidad(), Integer::sum);
            lineas.add(new LineaFactura(i.getProducto().getNombre(), i.getCantidad(), i.getProducto().getPrecioVenta()));
        }
        // 2) Se verifica todo antes de tocar el stock, para no dejar descuentos a medias.
        for (Map.Entry<String, Integer> e : necesarios.entrySet()) {
            if (!datos.getServicioStock().hayStock(e.getKey(), e.getValue()))
                throw new ReglaNegocioException("Stock insuficiente de " + e.getKey()
                        + " para confirmar la consulta (se necesitan " + e.getValue() + ")");
        }
        // 3) Cierre, descuento y facturación.
        c.cerrar();
        for (Map.Entry<String, Integer> e : necesarios.entrySet()) {
            datos.getServicioStock().descontarPorVencimiento(e.getKey(), e.getValue());
        }
        Factura f = new Factura(datos.tomarNumeroFactura(), LocalDate.now(), c);
        for (LineaFactura l : lineas) f.agregarLinea(l);
        datos.getFacturas().add(f);
        guardar();
        return f;
    }

    public void agregarNotaRectificatoria(Consulta c, String texto, Veterinario firmante) throws ReglaNegocioException {
        c.agregarNotaRectificatoria(texto, firmante);
        guardar();
    }

    public List<Consulta> historiaClinica(Mascota m) { return m.getHistoriaClinica(); }

    // ---------------------------------------------------------------- stock

    public Producto registrarMedicamento(String codigo, String nombre, int minimo, String principio, String presentacion)
            throws ReglaNegocioException {
        Medicamento m = crear(() -> new Medicamento(codigo, nombre, minimo, principio, presentacion));
        crear(() -> { datos.getServicioStock().registrarProducto(m); return m; });
        guardar();
        return m;
    }

    public Producto registrarInsumo(String codigo, String nombre, int minimo, String unidad) throws ReglaNegocioException {
        Insumo i = crear(() -> new Insumo(codigo, nombre, minimo, unidad));
        crear(() -> { datos.getServicioStock().registrarProducto(i); return i; });
        guardar();
        return i;
    }

    public void ingresarLote(String codigoProducto, String numeroLote, LocalDate vencimiento,
                             int cantidad, double costo) throws ReglaNegocioException {
        crear(() -> {
            datos.getServicioStock().ingresarLote(codigoProducto,
                    new LoteProducto(numeroLote, vencimiento, cantidad, costo));
            return null;
        });
        guardar();
    }

    public List<Producto> listarProductos() { return datos.getServicioStock().getProductos(); }
    public List<Producto> productosStockBajo() { return datos.getServicioStock().consultarStockBajo(); }

    // ---------------------------------------------------------------- facturación

    public List<Factura> listarFacturas() { return Collections.unmodifiableList(datos.getFacturas()); }

    public Factura buscarFactura(int numero) throws ReglaNegocioException {
        for (Factura f : datos.getFacturas()) if (f.getNumero() == numero) return f;
        throw new ReglaNegocioException("No existe la factura " + numero);
    }

    public PlanSalud planDe(Factura f) {
        return f.getConsulta() == null ? null : f.getConsulta().getMascota().getDuenoActual().getPlanSalud();
    }

    public double calcularTotal(Factura f, MedioPago medio) {
        return f.calcularTotal(planDe(f), medio);
    }

    public void pagarFactura(int numero, MedioPago medio) throws ReglaNegocioException {
        Factura f = buscarFactura(numero);
        crear(() -> {
            f.registrarPago(new Pago(LocalDate.now(), f.calcularTotal(planDe(f), medio), medio));
            return null;
        });
        guardar();
    }

    public NotaCredito anularFactura(int numero, String motivo) throws ReglaNegocioException {
        Factura f = buscarFactura(numero);
        NotaCredito nc = crear(() -> f.anular(datos.tomarNumeroNotaCredito(), motivo));
        guardar();
        return nc;
    }

    // ---------------------------------------------------------------- vacunas y reportes

    public List<Vacuna> listarCatalogoVacunas() { return Collections.unmodifiableList(datos.getCatalogoVacunas()); }

    public void enviarRecordatoriosVacunas() {
        servicioRecordatorios.avisarVacunasProximas(listarMascotas(), LocalDate.now());
    }

    /** Ejecuta cualquier reporte a través de la abstracción {@link Reporte} (no conoce las clases concretas). */
    public String emitirReporte(Reporte reporte) { return reporte.generarReporte(); }

    public Reporte reporteTurnos(Veterinario v) {
        return new ReporteTurnosVeterinario(v == null ? listarTurnos() : listarTurnosDe(v));
    }
    public Reporte reporteStockBajo() { return new ReporteStockBajo(datos.getServicioStock()); }
    public Reporte reporteVacunasProximas() {
        return new ReporteVacunasProximas(servicioRecordatorios, listarMascotas(), LocalDate.now());
    }

    // ---------------------------------------------------------------- utilidades

    /** Ejecuta una construcción del modelo y convierte las validaciones (IllegalArgument/State) en ReglaNegocioException. */
    private <T> T crear(java.util.function.Supplier<T> accion) throws ReglaNegocioException {
        try {
            return accion.get();
        } catch (IllegalArgumentException | IllegalStateException | NullPointerException ex) {
            throw new ReglaNegocioException(ex.getMessage() == null ? "Datos inválidos" : ex.getMessage());
        }
    }
}
