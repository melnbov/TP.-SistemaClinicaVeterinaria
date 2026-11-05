package modelo.entidades;

import java.io.Serializable;

/** Insumo o medicamento usado durante una consulta. Su stock se descuenta recién al confirmar la consulta. */
public class InsumoUtilizado implements Serializable {
    private final Producto producto;
    private final int cantidad;

    public InsumoUtilizado(Producto producto, int cantidad) {
        if (producto == null) throw new IllegalArgumentException("El producto es obligatorio");
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser positiva");
        this.producto = producto;
        this.cantidad = cantidad;
    }

    public Producto getProducto() { return producto; }
    public int getCantidad() { return cantidad; }

    @Override
    public String toString() {
        return producto.getNombre() + " x" + cantidad;
    }
}
