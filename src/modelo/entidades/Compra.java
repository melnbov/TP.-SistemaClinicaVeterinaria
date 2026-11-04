package modelo.entidades;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Compra implements Serializable {

    private final int numero;
    private final LocalDate fecha;
    private final Proveedor proveedor;
    private final List<DetalleCompra> detalles = new ArrayList<>();

    public Compra(int numero, LocalDate fecha, Proveedor proveedor) {
        if (numero <= 0 || fecha == null || proveedor == null)
            throw new IllegalArgumentException("Datos de compra inválidos");
        this.numero = numero;
        this.fecha = fecha;
        this.proveedor = proveedor;
    }

    public void agregarDetalle(DetalleCompra detalle) {
        if (detalle == null) throw new IllegalArgumentException("Detalle obligatorio");
        detalles.add(detalle);
    }

    public double calcularTotal() {
        double total = 0;
        for (DetalleCompra d : detalles) total += d.calcularSubtotal();
        return total;
    }

    public int getNumero() {
        return numero;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public List<DetalleCompra> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }

}