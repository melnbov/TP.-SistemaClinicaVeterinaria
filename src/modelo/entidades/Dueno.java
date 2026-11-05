package modelo.entidades;

import java.util.Objects;

public class Dueno implements Comparable<Dueno>, java.io.Serializable {

    private final String nombre;
    private final String apellido;
    private final String documento;
    private String telefono;
    private String email;
    private PlanSalud planSalud; // null = sin plan de salud animal

    public Dueno(String nombre, String apellido, String documento,
                 String telefono, String email) {

        this.nombre = requerirTexto(nombre, "El nombre");
        this.apellido = requerirTexto(apellido, "El apellido");
        this.documento = requerirTexto(documento, "El documento");

        setTelefono(telefono);
        setEmail(email);
    }

    private static String requerirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede estar vacío");
        }

        return valor.trim();
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getDocumento() {
        return documento;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEmail() {
        return email;
    }

    public PlanSalud getPlanSalud() {
        return planSalud;
    }

    public void setPlanSalud(PlanSalud planSalud) {
        this.planSalud = planSalud;
    }

    public void setTelefono(String telefono) {
        this.telefono = requerirTexto(telefono, "El teléfono");
    }

    public void setEmail(String email) {
        String correo = requerirTexto(email, "El email");

        if (!correo.contains("@")) {
            throw new IllegalArgumentException("El email no es válido: " + correo);
        }

        this.email = correo;
    }

    @Override
    public int compareTo(Dueno otro) {
        if (otro == null) {
            throw new IllegalArgumentException(
                    "El dueño a comparar no puede ser nulo");
        }

        int porApellido = apellido.compareToIgnoreCase(otro.apellido);

        if (porApellido != 0) {
            return porApellido;
        }

        int porNombre = nombre.compareToIgnoreCase(otro.nombre);

        if (porNombre != 0) {
            return porNombre;
        }

        return documento.compareToIgnoreCase(otro.documento);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Dueno)) {
            return false;
        }

        Dueno otro = (Dueno) o;
        return documento.equals(otro.documento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documento);
    }

    @Override
    public String toString() {
        return apellido + ", " + nombre + " (DNI " + documento + ")";
    }
}

