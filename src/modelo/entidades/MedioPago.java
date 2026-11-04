package modelo.entidades;

import java.io.Serializable;

public interface MedioPago extends Serializable {
    double calcularRecargo(double subtotal);
    String getNombre();
}

