package clinica.modelo.entidades;

import clinica.modelo.excepciones.ReglaNegocioException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Mascota {

    private final String nombre;
    private final String raza;
    private final String sexo;
    private final LocalDate fechaNacimiento;
    private double pesoActual;
    private Dueno duenoActual;

    private final List<HistorialDueno> historialDuenos = new ArrayList<>();
    private final List<Consulta> historiaClinica = new ArrayList<>();

    protected Mascota(String nombre, String raza, String sexo,
                      LocalDate fechaNacimiento, double pesoActual, Dueno dueno) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la mascota no puede estar vacío");
        }
        if (fechaNacimiento == null || fechaNacimiento.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no es válida");
        }
        if (pesoActual <= 0 || Double.isNaN(pesoActual) || Double.isInfinite(pesoActual)) {
            throw new IllegalArgumentException("El peso debe ser mayor a 0");
        }
        if (dueno == null) {
            throw new IllegalArgumentException("Toda mascota debe tener un dueño responsable");
        }

        this.nombre = nombre.trim();
        this.raza = raza;
        this.sexo = sexo;
        this.fechaNacimiento = fechaNacimiento;
        this.pesoActual = pesoActual;
        this.duenoActual = dueno;

        this.historialDuenos.add(new HistorialDueno(dueno, LocalDate.now()));
    }

    public abstract Especie getEspecie();


    public void cambiarDueno(Dueno nuevoDueno)
            throws ReglaNegocioException {
        cambiarDueno(nuevoDueno, LocalDate.now());
    }

    public void cambiarDueno(Dueno nuevoDueno, LocalDate fecha)
            throws ReglaNegocioException {

        if (nuevoDueno == null) {
            throw new ReglaNegocioException(
                    "El nuevo dueño no puede ser nulo");
        }

        if (fecha == null || fecha.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException(
                    "La fecha de transferencia no es válida");
        }

        if (nuevoDueno.equals(duenoActual)) {
            throw new ReglaNegocioException(
                    "El nuevo dueño es el mismo que el actual");
        }

        HistorialDueno vigente =
                historialDuenos.get(historialDuenos.size() - 1);

        if (fecha.isBefore(vigente.getFechaDesde())) {
            throw new ReglaNegocioException(
                    "La fecha no puede ser anterior al inicio de la tenencia");
        }

        Dueno duenoAnterior = duenoActual;
        boolean estabaRegistrada =
                duenoAnterior.getMascotas().contains(this);

        if (estabaRegistrada && nuevoDueno.getMascotas().contains(this)) {
            throw new ReglaNegocioException(
                    "La mascota ya está registrada para el nuevo dueño");
        }

        vigente.cerrar(fecha);

        if (estabaRegistrada) {
            duenoAnterior.quitarMascota(this);
        }

        duenoActual = nuevoDueno;
        historialDuenos.add(new HistorialDueno(nuevoDueno, fecha));

        if (estabaRegistrada) {
            nuevoDueno.registrarMascota(this);
        }
    }

    public Consulta registrarConsulta(Veterinario veterinario, String motivo) {
        return registrarConsulta(veterinario, motivo, LocalDate.now());
    }

    public Consulta registrarConsulta(Veterinario veterinario, String motivo,
                                      LocalDate fecha) {
        Consulta consulta = new Consulta(this, veterinario, motivo, fecha);
        historiaClinica.add(consulta);
        return consulta;
    }

    public List<Consulta> getHistoriaClinica() {
        return Collections.unmodifiableList(historiaClinica);
    }

    public List<VacunaAplicada> getVacunasAplicadas() {
        List<VacunaAplicada> todas = new ArrayList<>();

        for (Consulta consulta : historiaClinica) {
            todas.addAll(consulta.getVacunasAplicadas());
        }

        return Collections.unmodifiableList(todas);
    }

    public List<VacunaAplicada> getVacunasPendientesDeRecordatorio(
            LocalDate hoy) {

        if (hoy == null) {
            throw new IllegalArgumentException(
                    "La fecha de consulta es obligatoria");
        }

        List<VacunaAplicada> todas = getVacunasAplicadas();
        List<VacunaAplicada> pendientes = new ArrayList<>();

        for (VacunaAplicada vacuna : todas) {
            boolean tieneAplicacionPosterior = false;

            for (VacunaAplicada otra : todas) {
                if (vacuna.getVacuna().getCodigo()
                        .equals(otra.getVacuna().getCodigo())
                        && otra.getFechaAplicacion()
                        .isAfter(vacuna.getFechaAplicacion())) {

                    tieneAplicacionPosterior = true;
                    break;
                }
            }

            if (!tieneAplicacionPosterior
                    && vacuna.debeRecordarse(hoy)) {
                pendientes.add(vacuna);
            }
        }

        return Collections.unmodifiableList(pendientes);
    }

    public long getEdadEnSemanas(LocalDate al) {
        if (al == null || al.isBefore(fechaNacimiento)) {
            throw new IllegalArgumentException("La fecha de cálculo de edad no es válida");
        }

        return ChronoUnit.WEEKS.between(fechaNacimiento, al);
    }

    void actualizarPeso(double nuevoPeso) {
        if (nuevoPeso <= 0 || Double.isNaN(nuevoPeso) || Double.isInfinite(nuevoPeso)) {
            throw new IllegalArgumentException("El peso debe ser mayor a 0");
        }

        this.pesoActual = nuevoPeso;
    }

    public String getNombre() {
        return nombre;
    }

    public String getRaza() {
        return raza;
    }

    public String getSexo() {
        return sexo;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public double getPesoActual() {
        return pesoActual;
    }

    public Dueno getDuenoActual() {
        return duenoActual;
    }

    public List<HistorialDueno> getHistorialDuenos() {
        return Collections.unmodifiableList(historialDuenos);
    }

    @Override
    public String toString() {
        return nombre + " (" + getEspecie() + ", dueño: " + duenoActual + ")";
    }
}

