package modelo.servicios;

import modelo.excepciones.ReglaNegocioException;
import modelo.entidades.Dueno;
import modelo.entidades.Mascota;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import modelo.entidades.Perro;

public class RegistroDuenosMascotas implements java.io.Serializable {

    private final Map<String, Dueno> duenos = new HashMap<>();
    private final List<Mascota> mascotas = new ArrayList<>();

    public void registrarDueno(Dueno dueno)
            throws ReglaNegocioException {

        if (dueno == null) {
            throw new ReglaNegocioException(
                    "El dueño no puede ser nulo");
        }

        if (duenos.containsKey(dueno.getDocumento())) {
            throw new ReglaNegocioException(
                    "Ya existe un dueño con ese documento");
        }

        duenos.put(dueno.getDocumento(), dueno);
    }

    public void registrarMascota(Mascota mascota)
            throws ReglaNegocioException {

        if (mascota == null) {
            throw new ReglaNegocioException(
                    "La mascota no puede ser nula");
        }

        if (mascotas.contains(mascota)) {
            throw new ReglaNegocioException(
                    "La mascota ya está registrada en la clínica");
        }

        if (mascota instanceof Perro) {
            Perro perroNuevo = (Perro) mascota;

            for (Mascota registrada : mascotas) {
                if (registrada instanceof Perro) {
                    Perro perroRegistrado = (Perro) registrada;

                    if (perroRegistrado.getNumeroChip()
                            .equalsIgnoreCase(perroNuevo.getNumeroChip())) {
                        throw new ReglaNegocioException(
                                "Ya existe un perro registrado con ese número de chip");
                    }
                }
            }
        }

        Dueno dueno = mascota.getDuenoActual();

        if (dueno == null
                || !duenos.containsKey(dueno.getDocumento())) {
            throw new ReglaNegocioException(
                    "El dueño de la mascota no está registrado");
        }

        dueno.registrarMascota(mascota);
        mascotas.add(mascota);
    }

    public void transferirMascota(
            Mascota mascota, Dueno nuevoDueno, LocalDate fecha)
            throws ReglaNegocioException {

        if (mascota == null || !mascotas.contains(mascota)) {
            throw new ReglaNegocioException(
                    "La mascota no está registrada en la clínica");
        }

        if (nuevoDueno == null) {
            throw new ReglaNegocioException(
                    "El nuevo dueño debe estar registrado en la clínica");
        }

        Dueno duenoRegistrado =
                duenos.get(nuevoDueno.getDocumento());

        if (duenoRegistrado == null) {
            throw new ReglaNegocioException(
                    "El nuevo dueño debe estar registrado en la clínica");
        }

        mascota.cambiarDueno(duenoRegistrado, fecha);
    }

    public Optional<Dueno> buscarDueno(String documento) {
        return Optional.ofNullable(duenos.get(documento));
    }

    public List<Dueno> listarDuenos() {
        List<Dueno> lista = new ArrayList<>(duenos.values());
        Collections.sort(lista);
        return lista;
    }

    public List<Mascota> listarMascotas(
            Comparator<Mascota> orden) {

        List<Mascota> lista = new ArrayList<>(mascotas);

        if (orden != null) {
            lista.sort(orden);
        }

        return lista;
    }

    /** Lista todas las mascotas ordenadas por nombre. */
    public List<Mascota> listarMascotas() {
        return listarMascotasPorNombre();
    }

    public List<Mascota> listarMascotasPorNombre() {
        return listarMascotas(
                Comparator.comparing(
                        Mascota::getNombre,
                        String.CASE_INSENSITIVE_ORDER));
    }

    public List<Mascota> buscarMascotasPorNombre(String texto) {
        List<Mascota> resultado = new ArrayList<>();

        if (texto == null || texto.isBlank()) {
            return resultado;
        }

        String filtro = texto.trim().toLowerCase(java.util.Locale.ROOT);

        for (Mascota mascota : listarMascotasPorNombre()) {
            if (mascota.getNombre().toLowerCase(java.util.Locale.ROOT).contains(filtro)) {
                resultado.add(mascota);
            }
        }

        return resultado;
    }
}

