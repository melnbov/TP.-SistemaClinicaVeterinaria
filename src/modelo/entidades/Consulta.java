package modelo.entidades;

import modelo.excepciones.ReglaNegocioException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Consulta {

    private static final AtomicInteger SECUENCIA = new AtomicInteger(1);

    private final int numero;
    private final LocalDate fecha;
    private final Mascota mascota;
    private final Veterinario veterinario;
    private final String motivo;

    private double pesoDelDia;
    private String diagnostico;
    private String observaciones;
    private boolean cerrada;

    private final List<Tratamiento> tratamientos = new ArrayList<>();
    private final List<VacunaAplicada> vacunasAplicadas = new ArrayList<>();
    private final List<NotaRectificatoria> notas = new ArrayList<>();

    Consulta(Mascota mascota, Veterinario veterinario,
             String motivo, LocalDate fecha) {

        if (mascota == null) {
            throw new IllegalArgumentException("La mascota es obligatoria");
        }

        if (veterinario == null) {
            throw new IllegalArgumentException(
                    "Solo un veterinario puede registrar una consulta");
        }

        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                    "La consulta debe tener un motivo");
        }

        if (fecha == null || fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La fecha de la consulta no es válida");
        }

        this.numero = SECUENCIA.getAndIncrement();
        this.mascota = mascota;
        this.veterinario = veterinario;
        this.motivo = motivo.trim();
        this.fecha = fecha;
    }

    public void registrarPeso(double peso) throws ReglaNegocioException {
        asegurarAbierta();

        if (peso <= 0 || Double.isNaN(peso) || Double.isInfinite(peso)) {
            throw new ReglaNegocioException(
                    "El peso del día debe ser mayor a 0");
        }

        this.pesoDelDia = peso;
        mascota.actualizarPeso(peso);
    }

    public void registrarDiagnostico(String diagnostico, String observaciones)
            throws ReglaNegocioException {

        asegurarAbierta();

        if (diagnostico == null || diagnostico.isBlank()) {
            throw new ReglaNegocioException(
                    "El diagnóstico no puede estar vacío");
        }

        this.diagnostico = diagnostico.trim();

        this.observaciones =
                observaciones == null || observaciones.isBlank()
                        ? null : observaciones.trim();
    }

    public Tratamiento indicarTratamiento(
            Medicamento medicamento, String dosis,
            String frecuencia, String duracion)
            throws ReglaNegocioException {

        asegurarAbierta();

        if (pesoDelDia <= 0) {
            throw new ReglaNegocioException(
                    "No se puede indicar un medicamento sin registrar "
                            + "el peso del animal en esta consulta");
        }

        if (medicamento == null
                || dosis == null || dosis.isBlank()
                || frecuencia == null || frecuencia.isBlank()
                || duracion == null || duracion.isBlank()) {

            throw new ReglaNegocioException(
                    "El tratamiento requiere medicamento, dosis, "
                            + "frecuencia y duración");
        }

        Tratamiento tratamiento = new Tratamiento(
                medicamento, dosis.trim(),
                frecuencia.trim(), duracion.trim());

        tratamientos.add(tratamiento);
        return tratamiento;
    }

    public VacunaAplicada aplicarVacuna(Vacuna vacuna, String lote)
            throws ReglaNegocioException {

        asegurarAbierta();

        if (vacuna == null || lote == null || lote.isBlank()) {
            throw new ReglaNegocioException(
                    "La vacuna y el lote son obligatorios");
        }

        if (!vacuna.aplicaA(mascota.getEspecie())) {
            throw new ReglaNegocioException(
                    "La vacuna " + vacuna.getNombre()
                            + " no se aplica a la especie "
                            + mascota.getEspecie());
        }

        long edad = mascota.getEdadEnSemanas(fecha);

        if (edad < vacuna.getEdadMinimaSemanas()) {
            throw new ReglaNegocioException(
                    "La mascota tiene " + edad
                            + " semanas y la vacuna " + vacuna.getNombre()
                            + " requiere al menos "
                            + vacuna.getEdadMinimaSemanas() + " semanas");
        }

        VacunaAplicada aplicada = new VacunaAplicada(
                vacuna, lote.trim(), fecha);

        vacunasAplicadas.add(aplicada);
        return aplicada;
    }

    public void cerrar() throws ReglaNegocioException {
        asegurarAbierta();

        if (diagnostico == null || diagnostico.isBlank()) {
            throw new ReglaNegocioException(
                    "No se puede cerrar la consulta sin un diagnóstico");
        }

        cerrada = true;
    }

    public NotaRectificatoria agregarNotaRectificatoria(
            String texto, Veterinario firmante)
            throws ReglaNegocioException {

        if (!cerrada) {
            throw new ReglaNegocioException(
                    "Solo se pueden agregar notas rectificatorias a una consulta cerrada");
        }

        if (firmante == null) {
            throw new ReglaNegocioException(
                    "La nota rectificatoria debe estar firmada por un veterinario");
        }

        if (texto == null || texto.isBlank()) {
            throw new ReglaNegocioException(
                    "La nota rectificatoria no puede estar vacía");
        }

        NotaRectificatoria nota = new NotaRectificatoria(
                LocalDate.now(), texto.trim(), firmante);

        notas.add(nota);
        return nota;
    }

    private void asegurarAbierta() throws ReglaNegocioException {
        if (cerrada) {
            throw new ReglaNegocioException(
                    "La consulta " + numero
                            + " está cerrada: solo admite notas rectificatorias");
        }
    }

    public static void actualizarSecuencia(int ultimoNumero) {
        if (ultimoNumero < 0) {
            throw new IllegalArgumentException(
                    "El número de consulta no puede ser negativo"
            );
        }

        SECUENCIA.updateAndGet(
                actual -> Math.max(actual, Math.addExact(ultimoNumero,1))
        );
    }

    public int getNumero() {
        return numero;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public Mascota getMascota() {
        return mascota;
    }

    public Veterinario getVeterinario() {
        return veterinario;
    }

    public String getMotivo() {
        return motivo;
    }

    public double getPesoDelDia() {
        return pesoDelDia;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public boolean estaCerrada() {
        return cerrada;
    }

    public List<Tratamiento> getTratamientos() {
        return Collections.unmodifiableList(tratamientos);
    }

    public List<VacunaAplicada> getVacunasAplicadas() {
        return Collections.unmodifiableList(vacunasAplicadas);
    }

    public List<NotaRectificatoria> getNotasRectificatorias() {
        return Collections.unmodifiableList(notas);
    }

    @Override
    public String toString() {
        return "Consulta #" + numero + " (" + fecha + ") - " + motivo;
    }
}

