package modelo.entidades;

import java.io.Serializable;

/** Débito: sin recargo, usa la regla general de {@link MedioPago}. */
public class Debito implements Serializable, MedioPago {
    @Override
    public String getNombre() {
        return "Débito";
    }
}
