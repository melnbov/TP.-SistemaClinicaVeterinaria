package modelo.entidades;

import java.io.Serializable;

public class Efectivo implements Serializable, MedioPago {
    private static final double DESCUENTO_EFECTIVO = 10;

    /** El descuento por efectivo y el del plan de salud no se acumulan: se aplica el mayor. */
    @Override
    public double calcularTotal(double subtotal, double porcentajePlan) {
        double porcentaje = Math.max(porcentajePlan, DESCUENTO_EFECTIVO);
        return Math.max(0, subtotal - (subtotal * porcentaje / 100.0));
    }

    @Override
    public String getNombre() {
        return "Efectivo";
    }
}
