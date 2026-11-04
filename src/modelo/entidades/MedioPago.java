package modelo.entidades;

import java.io.Serializable;

/**
 * Medio de pago. Cada medio sabe cuánto debe pagarse con su propia regla, así que
 * {@link Factura} nunca pregunta por el tipo concreto: para agregar un medio nuevo
 * (billetera virtual, transferencia, etc.) alcanza con crear otra clase que implemente esta interfaz.
 */
public interface MedioPago extends Serializable {
    String getNombre();

    /** Recargo propio del medio sobre el importe ya descontado (0 por defecto). */
    default double calcularRecargo(double importeConDescuento) {
        return 0;
    }

    /**
     * Total a pagar con este medio.
     * Regla general: se aplica el descuento del plan de salud y luego el recargo del medio.
     * Un medio con comportamiento especial (p. ej. {@link Efectivo}) redefine este método.
     *
     * @param subtotal       subtotal de la factura, sin descuentos
     * @param porcentajePlan descuento del plan de salud del dueño (0 si no tiene)
     */
    default double calcularTotal(double subtotal, double porcentajePlan) {
        double conDescuento = subtotal - (subtotal * porcentajePlan / 100.0);
        return Math.max(0, conDescuento + calcularRecargo(conDescuento));
    }
}
