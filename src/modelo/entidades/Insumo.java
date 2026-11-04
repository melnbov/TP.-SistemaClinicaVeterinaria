package modelo.entidades;

import java.io.Serializable;

public class Insumo extends Producto implements Serializable {
    private final String unidadMedida;
    public Insumo(String codigo, String nombre, int stockMinimo, String unidadMedida) {
        super(codigo, nombre, stockMinimo);
        if (unidadMedida == null || unidadMedida.isBlank()) throw new IllegalArgumentException("La unidad de medida es obligatoria");
        this.unidadMedida = unidadMedida.trim();
    }
    public String getUnidadMedida() { return unidadMedida; }
}
