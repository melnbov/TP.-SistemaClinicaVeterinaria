package modelo.entidades;

import java.io.Serializable;
import java.time.LocalDate;

public class LoteProducto implements Serializable {
    private final String numeroLote;
    private final LocalDate fechaVencimiento;
    private int cantidadDisponible;
    private final double costoUnitario;

    public LoteProducto(String numeroLote, LocalDate fechaVencimiento, int cantidadDisponible, double costoUnitario) {
        if (numeroLote == null || numeroLote.isBlank())
            throw new IllegalArgumentException("El número de lote es obligatorio");
        if (fechaVencimiento == null)
            throw new IllegalArgumentException("La fecha de vencimiento es obligatoria");
        if (cantidadDisponible < 0 || costoUnitario < 0)
            throw new IllegalArgumentException("Cantidad y costo no pueden ser negativos");
        this.numeroLote = numeroLote.trim();
        this.fechaVencimiento = fechaVencimiento;
        this.cantidadDisponible = cantidadDisponible;
        this.costoUnitario = costoUnitario;
    }
    public boolean estaVencido() {
        return fechaVencimiento.isBefore(LocalDate.now());
    }
    public void descontar(int cantidad) {
        if (cantidad <= 0)
            throw new IllegalArgumentException("La cantidad debe ser positiva");
        if (estaVencido())
            throw new IllegalStateException("No se puede utilizar un lote vencido");
        if (cantidad > cantidadDisponible)
            throw new IllegalArgumentException("Stock insuficiente en el lote");
        cantidadDisponible -= cantidad;
    }
    public void ingresar(int cantidad) {
        if (cantidad <= 0)
            throw new IllegalArgumentException("La cantidad debe ser positiva");
        cantidadDisponible += cantidad;
    }
    public String getNumeroLote() {
        return numeroLote;
    }
    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }
    public int getCantidadDisponible() {
        return cantidadDisponible;
    }
    public double getCostoUnitario() {
        return costoUnitario;
    }
}
