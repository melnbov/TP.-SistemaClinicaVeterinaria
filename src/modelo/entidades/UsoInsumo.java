package modelo.entidades;

import java.io.Serializable;

public class UsoInsumo implements Serializable {
    private final LoteProducto lote;
    private final int cantidad;
    public UsoInsumo(LoteProducto lote, int cantidad) {
        if (lote == null)
            throw new IllegalArgumentException("El lote es obligatorio");
        if (cantidad <= 0)
            throw new IllegalArgumentException("La cantidad debe ser positiva");
        lote.descontar(cantidad);
        this.lote = lote;
        this.cantidad = cantidad;
    }
    public LoteProducto getLote() {
        return lote;
    }
    public int getCantidad() {
        return cantidad;
    }
}

