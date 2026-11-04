package modelo.entidades;

import java.io.Serializable;

public class Medicamento extends Producto implements Serializable {
    private final String principioActivo;
    private final String presentacion;

    public Medicamento(String codigo, String nombre, int stockMinimo, String principioActivo, String presentacion) {
        super(codigo, nombre, stockMinimo);
        if (principioActivo == null || principioActivo.isBlank())
            throw new IllegalArgumentException("El principio activo es obligatorio");
        if (presentacion == null || presentacion.isBlank())
            throw new IllegalArgumentException("La presentación es obligatoria");
        this.principioActivo = principioActivo.trim();
        this.presentacion = presentacion.trim();
    }
    public String getPrincipioActivo() {
        return principioActivo;
    }
    public String getPresentacion() {
        return presentacion;
    }
}

