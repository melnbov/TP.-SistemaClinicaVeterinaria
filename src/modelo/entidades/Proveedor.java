
package modelo.entidades;

import java.io.Serializable;

/*** Representa a un proveedor de productos e insumos de la clínica.*/
public class Proveedor implements Serializable {

    private final String nombre;
    private final String cuit;
    private final String telefono;
    private final String email;

    public Proveedor(String nombre, String cuit, String telefono, String email) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio");
        }
        if (cuit == null || cuit.isBlank()) {
            throw new IllegalArgumentException("El CUIT del proveedor es obligatorio");
        }
        if (telefono == null || telefono.isBlank()) {
            throw new IllegalArgumentException("El teléfono del proveedor es obligatorio");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("El email del proveedor no es válido");
        }

        this.nombre = nombre.trim();
        this.cuit = cuit.trim();
        this.telefono = telefono.trim();
        this.email = email.trim();
    }

    public String getNombre() {
        return nombre;
    }
    public String getCuit() {
        return cuit;
    }
    public String getTelefono() {
        return telefono;
    }
    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return nombre + " (CUIT: " + cuit + ")";
    }
}
