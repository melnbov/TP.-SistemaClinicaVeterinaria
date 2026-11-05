package modelo.entidades;

/**
 * Tipos de atención. La duración sale del enunciado; el precio base es un
 * SUPUESTO del grupo (el enunciado no lo define) y se usa para facturar y
 * para calcular el 20 % de cargo por cancelación tardía.
 */
public enum TipoAtencion {
    CONSULTA_GENERAL(30, 15000.0),
    VACUNACION(15, 8000.0),
    CIRUGIA(120, 120000.0);

    private final int duracionMinutos;
    private final double precio;

    TipoAtencion(int duracionMinutos, double precio) {
        this.duracionMinutos = duracionMinutos;
        this.precio = precio;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public double getPrecio() {
        return precio;
    }
}
